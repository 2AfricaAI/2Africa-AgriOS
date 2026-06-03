package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.service.AnalyticsService;
import ai.toafrica.agrios.service.vo.AgentLeaderboardVO;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 49 -- CS-Core Analytics dashboard endpoints.
 *
 * <p>v1 ships a single {@code overview} endpoint that returns KPI cards,
 * channel distribution, status distribution and a daily time series in
 * one round-trip. Future endpoints (per-agent leaderboard, per-inbox SLA,
 * AI auto-reply ratio) will land in Sprint 50+.</p>
 */
@Slf4j
@Tag(name = "92 - CS - Analytics", description = "Customer-service dashboard data")
@RestController
@RequestMapping({"/v1/cs/analytics", "/v1/service/analytics"})
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Overview snapshot for the CS dashboard")
    @GetMapping("/overview")
    public R<AnalyticsOverviewVO> overview(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        return R.ok(analyticsService.overview(days == null ? 30 : days));
    }

    /**
     * Sprint 50c -- per-agent SLA leaderboard. One row per agent with
     * assignment / resolution counts plus personal FRT + TTR. Sorted
     * by resolvedCount desc by default (frontend can re-sort).
     */
    @Operation(summary = "Per-agent SLA leaderboard")
    @GetMapping("/agents")
    public R<AgentLeaderboardVO> agentLeaderboard(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        return R.ok(analyticsService.agentLeaderboard(days == null ? 30 : days));
    }
}
