package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.DistributionSlice;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.TimeSeriesPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprint 49 -- CS-Core Analytics aggregator.
 *
 * <p>MVP version: pulls the open + resolved + pending pages from Chatwoot
 * once, then computes everything in-memory. Good enough for a single farm's
 * volume (low hundreds of conversations per day). When a tenant grows past
 * ~10k conversations we will swap this for a materialized table refreshed
 * by a nightly job -- see Sprint 53 roadmap.</p>
 *
 * <p>Caches each {@code overview(days)} answer for 5 minutes; the
 * dashboard polls at most every minute so this absorbs the load.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ChatwootClient chatwoot;

    /** TTL for the in-memory overview cache (ms). */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** Statuses we fetch + aggregate. Snoozed is rare; ignore for v1. */
    private static final List<String> STATUSES = List.of("open", "pending", "resolved");

    /** ISO date formatter shared across the buckets. */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Map<Integer, CachedOverview> cache = new ConcurrentHashMap<>();

    /**
     * Returns the analytics overview for the past {@code days} days.
     * Cached for {@link #CACHE_TTL_MS}.
     */
    public AnalyticsOverviewVO overview(int days) {
        int window = Math.max(1, Math.min(days, 365));

        CachedOverview cached = cache.get(window);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.builtAt < CACHE_TTL_MS) {
            return cached.value;
        }

        AnalyticsOverviewVO fresh = compute(window);
        cache.put(window, new CachedOverview(fresh, now));
        return fresh;
    }

    /** Force-rebuild the cache (used after a known-write, e.g. resolve). */
    public void invalidate() {
        cache.clear();
    }

    // ---------- internals ----------

    private AnalyticsOverviewVO compute(int window) {
        log.debug("[analytics] computing overview for window={}", window);

        // Fetch one page per status. Page 1 returns up to 25 conversations
        // (Chatwoot default). MVP doesn't paginate; we'll add it when a
        // single inbox exceeds the page size.
        List<ChatwootConversation> open = fetchStatus("open");
        List<ChatwootConversation> pending = fetchStatus("pending");
        List<ChatwootConversation> resolved = fetchStatus("resolved");

        // Cutoff for "is this within the window"
        long cutoffEpoch = LocalDate.now().minusDays(window)
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond();

        // KPI counters
        int openCount = open.size();
        int pendingCount = pending.size();
        int resolvedInWindow = (int) resolved.stream()
                .filter(c -> safeEpoch(c.getCreatedAt()) >= cutoffEpoch)
                .count();
        int totalInWindow = (int) concat(open, pending, resolved).stream()
                .filter(c -> safeEpoch(c.getCreatedAt()) >= cutoffEpoch)
                .count();

        // Distribution by status -- use snapshot counts (open/pending now,
        // resolved within window) so the totals match the KPIs above.
        List<DistributionSlice> byStatus = new ArrayList<>();
        if (openCount > 0)        byStatus.add(slice("open", "open", openCount));
        if (pendingCount > 0)     byStatus.add(slice("pending", "pending", pendingCount));
        if (resolvedInWindow > 0) byStatus.add(slice("resolved", "resolved", resolvedInWindow));

        // Distribution by channel (across the windowed set so it matches "total").
        Map<String, Integer> channelCounts = new HashMap<>();
        concat(open, pending, resolved).forEach(c -> {
            if (safeEpoch(c.getCreatedAt()) < cutoffEpoch) return;
            String channel = c.resolvedChannel();
            if (channel == null) channel = "unknown";
            channelCounts.merge(channel, 1, Integer::sum);
        });
        List<DistributionSlice> byChannel = channelCounts.entrySet().stream()
                .map(e -> slice(e.getKey(), humanChannel(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(DistributionSlice::getValue).reversed())
                .toList();

        // Daily time series across the window. Bucket by createdAt date.
        Map<String, Integer> dailyCounts = new HashMap<>();
        LocalDate today = LocalDate.now();
        // Pre-seed every day so the chart shows zeros instead of holes.
        for (int i = window - 1; i >= 0; i--) {
            dailyCounts.put(today.minusDays(i).format(ISO_DATE), 0);
        }
        concat(open, pending, resolved).forEach(c -> {
            long epoch = safeEpoch(c.getCreatedAt());
            if (epoch < cutoffEpoch) return;
            LocalDate d = Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate();
            String key = d.format(ISO_DATE);
            // Only count days that fall inside the seeded window.
            dailyCounts.computeIfPresent(key, (k, v) -> v + 1);
        });
        List<TimeSeriesPoint> dailyConversations = dailyCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> TimeSeriesPoint.builder().date(e.getKey()).count(e.getValue()).build())
                .toList();

        return AnalyticsOverviewVO.builder()
                .windowDays(window)
                .totalConversations(totalInWindow)
                .openConversations(openCount)
                .pendingConversations(pendingCount)
                .resolvedConversations(resolvedInWindow)
                .byStatus(byStatus)
                .byChannel(byChannel)
                .dailyConversations(dailyConversations)
                .build();
    }

    private List<ChatwootConversation> fetchStatus(String status) {
        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("status", status);
            filters.put("page", "1");
            return chatwoot.listConversations(filters);
        } catch (Exception e) {
            log.warn("[analytics] fetch status={} failed: {}", status, e.getMessage());
            return List.of();
        }
    }

    @SafeVarargs
    private static <T> List<T> concat(List<T>... lists) {
        List<T> out = new ArrayList<>();
        for (List<T> l : lists) out.addAll(l);
        return out;
    }

    private static long safeEpoch(Long epochSec) {
        return epochSec == null ? 0L : epochSec;
    }

    private static DistributionSlice slice(String key, String label, int value) {
        return DistributionSlice.builder().key(key).label(label).value(value).build();
    }

    private static String humanChannel(String channel) {
        if (channel == null) return "—";
        return channel.replace("Channel::", "");
    }

    private record CachedOverview(AnalyticsOverviewVO value, long builtAt) {}
}
