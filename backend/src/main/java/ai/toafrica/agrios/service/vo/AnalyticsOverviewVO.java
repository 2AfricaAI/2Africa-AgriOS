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

    // ---------- Sprint 50a: First Response Time (FRT) ----------
    /**
     * First Response Time metrics across all conversations in the window
     * that have BOTH a customer-side inbound AND at least one agent reply
     * after it. Conversations without an agent reply yet are excluded.
     */
    private FrtMetrics frtMetrics;

    // ---------- Sprint 50b: Time-To-Resolution (TTR) ----------
    /**
     * Time-To-Resolution metrics across conversations that were both
     * CREATED and RESOLVED inside the window. TTR is computed as
     * lastActivityAt - createdAt -- approximate but cheap (no extra
     * fetches). A future refinement will read the explicit "resolved"
     * activity timestamp from the message stream.
     */
    private TtrMetrics ttrMetrics;

    // ---------- Sprint 50d: CSAT (Customer Satisfaction) ----------
    /**
     * CSAT summary across responses SUBMITTED inside the window. Counts
     * only submitted ratings -- unanswered survey invitations are
     * excluded so the metric reflects actual customer voice and not
     * delivery rate. Null fields mean "no submissions yet".
     */
    private CsatMetrics csatMetrics;

    @Data
    @Builder
    public static class FrtMetrics {
        /** Average first-response time in seconds. */
        private Long avgSec;
        /** Median (P50) first-response time in seconds. */
        private Long p50Sec;
        /** P90 first-response time in seconds. */
        private Long p90Sec;
        /** Number of conversations that contributed to the metric. */
        private Integer sampleSize;
    }

    /**
     * Sprint 50d -- CSAT roll-up for the dashboard card. Numbers come
     * straight from {@code cs_csat_response} where
     * {@code rating IS NOT NULL AND submitted_at &gt;= cutoff}.
     */
    @Data
    @Builder
    public static class CsatMetrics {
        /** Submitted responses in the window. */
        private Integer sampleSize;
        /** Mean rating across the sample, rounded to 1 dp. Null if sample=0. */
        private Double avgRating;
        /** % of responses with rating &gt;= 4. Null if sample=0. */
        private Integer thumbsUpPct;
        /** Raw count behind the % (handy when sample is small). */
        private Integer thumbsUpCount;
    }

    /**
     * Shape mirrors FrtMetrics so the frontend can reuse the same KPI
     * card template. Kept as a sibling type (rather than a shared
     * "DurationMetrics") so we have room to add TTR-specific fields
     * later (e.g. reopen count, SLA-tier breakdown).
     */
    @Data
    @Builder
    public static class TtrMetrics {
        /** Average time-to-resolution in seconds. */
        private Long avgSec;
        /** Median (P50) time-to-resolution in seconds. */
        private Long p50Sec;
        /** P90 time-to-resolution in seconds. */
        private Long p90Sec;
        /** Number of resolved conversations that contributed. */
        private Integer sampleSize;
    }

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
