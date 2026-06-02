package ai.toafrica.agrios.service.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Sprint 49 -- CS-Core Analytics overview snapshot for the dashboard.
 *
 * <p>One round-trip returns the four KPI counters + two distributions +
 * one daily time series. Frontend renders this directly into the
 * Analytics dashboard without further aggregation.</p>
 *
 * <p>Computed by {@code AnalyticsService} from a single page of Chatwoot
 * conversations. Cached for a short TTL (5 min) because real-time
 * accuracy isn't worth the load.</p>
 */
@Data
@Builder
public class AnalyticsOverviewVO {

    /** Time window in days for the time series + range counts. */
    private Integer windowDays;

    // ---------- KPI cards (top-of-dashboard) ----------
    /** Total conversations created within the window. */
    private Integer totalConversations;
    /** Currently open right now (snapshot, not windowed). */
    private Integer openConversations;
    /** Currently pending right now. */
    private Integer pendingConversations;
    /** Resolved within the window. */
    private Integer resolvedConversations;

    // ---------- Distributions ----------
    /** Conversation count per channel, e.g. {Channel::WebWidget: 5, Channel::Email: 8}. */
    private List<DistributionSlice> byChannel;
    /** Conversation count per status. */
    private List<DistributionSlice> byStatus;

    // ---------- Time series ----------
    /** One bucket per day in the window, oldest first. */
    private List<TimeSeriesPoint> dailyConversations;

    @Data
    @Builder
    public static class DistributionSlice {
        /** Key as it appears in the source data, e.g. "Channel::WebWidget" or "open". */
        private String key;
        /** Human label after stripping the Channel:: prefix etc. */
        private String label;
        /** Count of conversations falling in this slice. */
        private Integer value;
    }

    @Data
    @Builder
    public static class TimeSeriesPoint {
        /** ISO-8601 date string, e.g. "2026-05-15". */
        private String date;
        /** Conversation count opened on that day. */
        private Integer count;
    }
}
