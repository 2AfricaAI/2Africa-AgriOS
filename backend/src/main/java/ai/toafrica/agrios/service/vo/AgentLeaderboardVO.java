package ai.toafrica.agrios.service.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Sprint 50c -- Per-agent SLA leaderboard.
 *
 * <p>For each agent that handled at least one conversation in the window,
 * we surface their assignment / resolution counts plus personal FRT and
 * TTR percentiles. The frontend renders this as a sortable table so a
 * manager can see at a glance who is fastest, who is slowest, and who is
 * carrying the most load.</p>
 *
 * <p>Unassigned conversations are aggregated into a single pseudo-row
 * with agentId=null + agentName="Unassigned" so the leaderboard doesn't
 * miss volume that nobody owned.</p>
 *
 * <p>Computed by {@code AnalyticsService.agentLeaderboard(days)} from a
 * single page of windowed conversations; same 5-minute cache as the
 * dashboard overview.</p>
 */
@Data
@Builder
public class AgentLeaderboardVO {

    /** Window in days the leaderboard was computed over. */
    private Integer windowDays;

    /** One row per agent that touched at least one conversation. */
    private List<AgentRow> rows;

    @Data
    @Builder
    public static class AgentRow {
        /** Chatwoot agent id; null for the synthetic "Unassigned" row. */
        private Long agentId;
        /** Display name (e.g. "Albert Mwangi") or "Unassigned" for orphans. */
        private String agentName;
        /** Avatar URL from Chatwoot if available; null otherwise. */
        private String thumbnail;
        /** "agent" / "administrator". Unassigned row uses null. */
        private String role;

        // ---------- volume ----------
        /** Conversations assigned to this agent in the window. */
        private Integer assignedCount;
        /** Subset of `assignedCount` that ended up resolved in the window. */
        private Integer resolvedCount;

        // ---------- response-time KPIs ----------
        /** Personal FRT average in seconds; null if the agent never replied. */
        private Long frtAvgSec;
        /** Personal FRT median (P50). */
        private Long frtP50Sec;
        /** Conversations that contributed to the agent's FRT metric. */
        private Integer frtSampleSize;

        // ---------- resolution-time KPIs ----------
        /** Personal TTR average in seconds across their resolved conversations. */
        private Long ttrAvgSec;
        /** Personal TTR median (P50). */
        private Long ttrP50Sec;
        /** Conversations that contributed to the agent's TTR metric. */
        private Integer ttrSampleSize;
    }
}
