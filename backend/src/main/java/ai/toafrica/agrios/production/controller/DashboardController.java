package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.service.DashboardService;
import ai.toafrica.agrios.production.vo.DashboardSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "00 · 首页 Dashboard", description = "首页摘要 - 一次返回所有 KPI 和列表")
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Dashboard 首页摘要")
    @GetMapping("/summary")
    public R<DashboardSummaryVO> summary() {
        return R.ok(dashboardService.summary());
    }
}
