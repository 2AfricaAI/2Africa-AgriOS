package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootAgent;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.vo.AgentLeaderboardVO;
import ai.toafrica.agrios.service.vo.AgentLeaderboardVO.AgentRow;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.CsatMetrics;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.DistributionSlice;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.FrtMetrics;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.TimeSeriesPoint;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO.TtrMetrics;
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
    /** Sprint 50d -- injected so the dashboard overview can also carry CSAT. */
    private final CsatService csatService;

    /** TTL for the in-memory overview cache (ms). */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** Statuses we fetch + aggregate. Snoozed is rare; ignore for v1. */
    private static final List<String> STATUSES = List.of("open", "pending", "resolved");

    /** ISO date formatter shared across the buckets. */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Map<Integer, CachedOverview> cache = new ConcurrentHashMap<>();
    /** Sprint 50c -- separate cache for the agent leaderboard. */
    private final Map<Integer, CachedLeaderboard> leaderboardCache = new ConcurrentHashMap<>();

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
        leaderboardCache.clear();
    }

    /**
     * Sprint 50c -- per-agent SLA leaderboard for the past {@code days}
     * days. Same 5-minute cache as {@link #overview(int)}.
     */
    public AgentLeaderboardVO agentLeaderboard(int days) {
        int window = Math.max(1, Math.min(days, 365));

        CachedLeaderboard cached = leaderboardCache.get(window);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.builtAt < CACHE_TTL_MS) {
            return cached.value;
        }

        AgentLeaderboardVO fresh = computeLeaderboard(window);
        leaderboardCache.put(window, new CachedLeaderboard(fresh, now));
        return fresh;
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

        // Sprint 50a: First Response Time across windowed conversations.
        FrtMetrics frt = computeFrt(
                concat(open, pending, resolved).stream()
                        .filter(c -> safeEpoch(c.getCreatedAt()) >= cutoffEpoch)
                        .toList()
        );

        // Sprint 50b: Time-To-Resolution across conversations RESOLVED
        // in the window. Restricted to the `resolved` page (status is
        // currently resolved) and to those whose createdAt is also in
        // the window so the comparison is apples-to-apples with FRT.
        TtrMetrics ttr = computeTtr(
                resolved.stream()
                        .filter(c -> safeEpoch(c.getCreatedAt()) >= cutoffEpoch)
                        .toList()
        );

        // Sprint 50d -- CSAT roll-up. Cheap DB scan; no extra Chatwoot calls.
        CsatService.CsatSummary csatSummary = csatService.computeSummary(window);
        CsatMetrics csat = CsatMetrics.builder()
                .sampleSize(csatSummary.sampleSize())
                .avgRating(csatSummary.avgRating())
                .thumbsUpPct(csatSummary.thumbsUpPct())
                .thumbsUpCount(csatSummary.thumbsUpCount())
                .build();

        return AnalyticsOverviewVO.builder()
                .windowDays(window)
                .totalConversations(totalInWindow)
                .openConversations(openCount)
                .pendingConversations(pendingCount)
                .resolvedConversations(resolvedInWindow)
                .byStatus(byStatus)
                .byChannel(byChannel)
                .dailyConversations(dailyConversations)
                .frtMetrics(frt)
                .ttrMetrics(ttr)
                .csatMetrics(csat)
                .build();
    }

    // ---------- FRT (Sprint 50a) ----------

    /**
     * For each conversation: fetch messages, find the first customer-side
     * inbound (messageType=0, not a private note), then the earliest agent
     * reply (messageType=1, not private) that occurred strictly after it.
     * Returns the delta in seconds.
     *
     * <p>Conversations with no agent reply yet, or with only outbound
     * messages, are skipped. Caches list of message fetches in a single
     * round-trip per conversation.</p>
     *
     * <p>For a single farm this is ~12-30 conversations per window, so 30
     * extra API calls is acceptable. When a tenant grows past ~500 active
     * conversations we will replace with a Chatwoot v2 reports API call.</p>
     */
    private FrtMetrics computeFrt(List<ChatwootConversation> windowed) {
        if (windowed.isEmpty()) {
            return FrtMetrics.builder().sampleSize(0).build();
        }

        List<Long> samples = new ArrayList<>();
        for (ChatwootConversation c : windowed) {
            Long delta = firstResponseSec(c.getId());
            if (delta != null) samples.add(delta);
        }
        if (samples.isEmpty()) {
            return FrtMetrics.builder().sampleSize(0).build();
        }

        samples.sort(Long::compareTo);
        long sum = 0;
        for (Long s : samples) sum += s;
        long avg = sum / samples.size();
        long p50 = samples.get(percentileIndex(samples.size(), 50));
        long p90 = samples.get(percentileIndex(samples.size(), 90));

        return FrtMetrics.builder()
                .avgSec(avg)
                .p50Sec(p50)
                .p90Sec(p90)
                .sampleSize(samples.size())
                .build();
    }

    /** Returns first-response seconds for a conversation, or null if no agent reply yet. */
    private Long firstResponseSec(Long conversationId) {
        try {
            List<ChatwootClient.ChatMessage> messages = chatwoot.listMessages(conversationId);
            // Chatwoot returns messages newest-first; flip to chronological so we
            // can scan forward for the boundary.
            List<ChatwootClient.ChatMessage> chrono = new ArrayList<>(messages);
            chrono.sort(Comparator.comparing(m -> safeEpoch(m.createdAt)));

            Long firstInbound = null;
            for (ChatwootClient.ChatMessage m : chrono) {
                if (m == null) continue;
                if (m.privateNote) continue;            // skip internal notes
                if (m.fromCustomer) {                   // messageType == 0
                    if (firstInbound == null) firstInbound = safeEpoch(m.createdAt);
                } else {                                // messageType == 1
                    // first outbound after the first inbound -> FRT delta
                    if (firstInbound != null) {
                        long delta = safeEpoch(m.createdAt) - firstInbound;
                        if (delta >= 0) return delta;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("[analytics] FRT lookup failed for conv#{}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    /**
     * Nearest-rank percentile index into a 0-indexed sorted list of size {@code n}.
     * e.g. P50 of [1,2,3,4,5] -> index 2 (value 3). P90 of size 10 -> index 8.
     */
    private static int percentileIndex(int n, int p) {
        if (n <= 0) return 0;
        int idx = (int) Math.ceil(n * (p / 100.0)) - 1;
        return Math.max(0, Math.min(n - 1, idx));
    }

    // ---------- TTR (Sprint 50b) ----------

    /**
     * Time-To-Resolution for the set of conversations that ended their
     * lifecycle inside the window.
     *
     * <p>MVP definition: {@code lastActivityAt - createdAt}. This is an
     * approximation -- a customer who pings the resolved thread later
     * (e.g. "thanks!") would push lastActivityAt and inflate TTR. In
     * practice, Chatwoot does not auto-reopen on a customer reply
     * unless the channel is configured to, so the drift is small.
     *
     * <p>Negative deltas (clock skew, malformed timestamps) are
     * discarded so they don't poison the average.</p>
     *
     * <p>Future iteration (Sprint 53 candidate) -- replace with an
     * explicit scan of the message stream for the message_type=2
     * activity row that carries the "Conversation was marked
     * resolved" content. That requires exposing message_type on
     * ChatMessage so we can distinguish activity rows from real
     * messages -- punted for now to keep this commit small.</p>
     */
    private TtrMetrics computeTtr(List<ChatwootConversation> resolvedInWindow) {
        if (resolvedInWindow.isEmpty()) {
            return TtrMetrics.builder().sampleSize(0).build();
        }

        List<Long> samples = new ArrayList<>();
        for (ChatwootConversation c : resolvedInWindow) {
            long created = safeEpoch(c.getCreatedAt());
            long last = safeEpoch(c.getLastActivityAt());
            if (created <= 0 || last <= 0) continue;
            long delta = last - created;
            if (delta < 0) continue;        // skew defence
            samples.add(delta);
        }
        if (samples.isEmpty()) {
            return TtrMetrics.builder().sampleSize(0).build();
        }

        samples.sort(Long::compareTo);
        long sum = 0;
        for (Long s : samples) sum += s;
        long avg = sum / samples.size();
        long p50 = samples.get(percentileIndex(samples.size(), 50));
        long p90 = samples.get(percentileIndex(samples.size(), 90));

        return TtrMetrics.builder()
                .avgSec(avg)
                .p50Sec(p50)
                .p90Sec(p90)
                .sampleSize(samples.size())
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
    private record CachedLeaderboard(AgentLeaderboardVO value, long builtAt) {}

    // ---------- Agent leaderboard (Sprint 50c) ----------

    /**
     * Build one row per agent across windowed conversations, plus a
     * synthetic "Unassigned" row for conversations nobody owned.
     *
     * <p>FRT per-agent reuses {@link #firstResponseSec(Long)} (so the
     * definition matches the dashboard headline -- first inbound to
     * first non-private outbound). TTR per-agent reuses the dashboard
     * approximation {@code lastActivityAt - createdAt}. Both are
     * documented limitations; see Sprint 50a / 50b commit notes.</p>
     */
    private AgentLeaderboardVO computeLeaderboard(int window) {
        log.debug("[analytics] computing agent leaderboard for window={}", window);

        List<ChatwootConversation> open = fetchStatus("open");
        List<ChatwootConversation> pending = fetchStatus("pending");
        List<ChatwootConversation> resolved = fetchStatus("resolved");

        long cutoffEpoch = LocalDate.now().minusDays(window)
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond();

        List<ChatwootConversation> windowed = concat(open, pending, resolved).stream()
                .filter(c -> safeEpoch(c.getCreatedAt()) >= cutoffEpoch)
                .toList();

        // Build an agent metadata map up front so every row can carry the
        // display name + avatar. Unknown ids (deactivated agents whose
        // history still lingers) fall back to "Agent #<id>".
        Map<Long, ChatwootAgent> agentIndex = new HashMap<>();
        try {
            for (ChatwootAgent a : chatwoot.listAgents()) {
                if (a != null && a.getId() != null) agentIndex.put(a.getId(), a);
            }
        } catch (Exception e) {
            log.warn("[analytics] listAgents failed: {}", e.getMessage());
        }

        // Group conversations by assigneeId. null key buckets the
        // Unassigned pseudo-row.
        Map<Long, List<ChatwootConversation>> byAgent = new HashMap<>();
        for (ChatwootConversation c : windowed) {
            byAgent.computeIfAbsent(c.getAssigneeId(), k -> new ArrayList<>()).add(c);
        }

        List<AgentRow> rows = new ArrayList<>();
        for (Map.Entry<Long, List<ChatwootConversation>> entry : byAgent.entrySet()) {
            Long agentId = entry.getKey();
            List<ChatwootConversation> group = entry.getValue();

            int assigned = group.size();
            int resolvedN = (int) group.stream()
                    .filter(c -> "resolved".equalsIgnoreCase(c.getStatus()))
                    .count();

            FrtMetrics frt = computeFrt(group);
            TtrMetrics ttr = computeTtr(group.stream()
                    .filter(c -> "resolved".equalsIgnoreCase(c.getStatus()))
                    .toList());

            String name;
            String thumbnail = null;
            String role = null;
            if (agentId == null) {
                name = "Unassigned";
            } else {
                ChatwootAgent a = agentIndex.get(agentId);
                if (a != null) {
                    name = a.getName() != null && !a.getName().isBlank()
                            ? a.getName()
                            : ("Agent #" + agentId);
                    thumbnail = a.getThumbnail();
                    role = a.getRole();
                } else {
                    name = "Agent #" + agentId;
                }
            }

            rows.add(AgentRow.builder()
                    .agentId(agentId)
                    .agentName(name)
                    .thumbnail(thumbnail)
                    .role(role)
                    .assignedCount(assigned)
                    .resolvedCount(resolvedN)
                    .frtAvgSec(frt.getAvgSec())
                    .frtP50Sec(frt.getP50Sec())
                    .frtSampleSize(frt.getSampleSize())
                    .ttrAvgSec(ttr.getAvgSec())
                    .ttrP50Sec(ttr.getP50Sec())
                    .ttrSampleSize(ttr.getSampleSize())
                    .build());
        }

        // Default ordering: most resolved first, then most assigned.
        // Unassigned (agentId=null) gets pushed to the bottom because
        // it's an aggregate, not a person, and shouldn't visually
        // outrank a top performer.
        rows.sort((a, b) -> {
            boolean aPseudo = a.getAgentId() == null;
            boolean bPseudo = b.getAgentId() == null;
            if (aPseudo != bPseudo) return aPseudo ? 1 : -1;
            int byResolved = Integer.compare(
                    b.getResolvedCount() == null ? 0 : b.getResolvedCount(),
                    a.getResolvedCount() == null ? 0 : a.getResolvedCount());
            if (byResolved != 0) return byResolved;
            return Integer.compare(
                    b.getAssignedCount() == null ? 0 : b.getAssignedCount(),
                    a.getAssignedCount() == null ? 0 : a.getAssignedCount());
        });

        return AgentLeaderboardVO.builder()
                .windowDays(window)
                .rows(rows)
                .build();
    }
}
