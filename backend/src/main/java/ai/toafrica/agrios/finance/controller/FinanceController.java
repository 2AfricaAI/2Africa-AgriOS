package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.service.CostAllocationService;
import ai.toafrica.agrios.finance.vo.PlanPnLVO;
import ai.toafrica.agrios.finance.vo.PnLListRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 12 - 财务报表入口.
 *
 * 这是 V2.0 Phase 2 "管理 + 财务一体化" 的对外 API 总入口.
 * 现阶段只提供 plan / batch 两个粒度的 P&L; Sprint 13-14 会扩到 plot / sku / 渠道 / 事业部.
 */
@Tag(name = "50 · Finance & P&L", description = "Cost allocation + P&L reports (V2.0 Phase 2)")
@RestController
@RequestMapping("/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final CostAllocationService costService;

    @Operation(summary = "Plan-level P&L (cost breakdown + revenue + gross margin)")
    @GetMapping("/pnl/plan/{planId}")
    public R<PlanPnLVO> getPlanPnL(@PathVariable Long planId) {
        return R.ok(costService.getPlanPnL(planId));
    }

    @Operation(summary = "Batch-level P&L (cost prorated from plan, revenue is actual)")
    @GetMapping("/pnl/batch/{batchId}")
    public R<PlanPnLVO> getBatchPnL(@PathVariable Long batchId) {
        return R.ok(costService.getBatchPnL(batchId));
    }

    @Operation(summary = "Plot-level P&L (cost SUM by plot_id with optional date range)")
    @GetMapping("/pnl/plot/{plotId}")
    public R<PlanPnLVO> getPlotPnL(
            @PathVariable Long plotId,
            @Parameter(description = "Cost period start (inclusive)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Cost period end (inclusive)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(costService.getPlotPnL(plotId, from, to));
    }

    @Operation(summary = "SKU-level P&L (cost = Σ plan_cost_per_kg × sold_kg)")
    @GetMapping("/pnl/sku/{skuId}")
    public R<PlanPnLVO> getSkuPnL(@PathVariable Long skuId) {
        return R.ok(costService.getSkuPnL(skuId));
    }

    // ============================================================
    // 列表视图 - 报表页面用
    // ============================================================

    @Operation(summary = "All plans with P&L summary")
    @GetMapping("/reports/plans")
    public R<List<PnLListRow>> listPlanPnL() {
        return R.ok(costService.listPlanPnL());
    }

    @Operation(summary = "All plots with P&L summary")
    @GetMapping("/reports/plots")
    public R<List<PnLListRow>> listPlotPnL() {
        return R.ok(costService.listPlotPnL());
    }

    @Operation(summary = "All SKUs with P&L summary (only SKUs with revenue)")
    @GetMapping("/reports/skus")
    public R<List<PnLListRow>> listSkuPnL() {
        return R.ok(costService.listSkuPnL());
    }

    @Operation(summary = "All customers with revenue contribution (Sprint 14)")
    @GetMapping("/reports/customers")
    public R<List<PnLListRow>> listCustomerPnL() {
        return R.ok(costService.listCustomerPnL());
    }

    @Operation(summary = "Channels (customer.type) with revenue rollup")
    @GetMapping("/reports/channels")
    public R<List<PnLListRow>> listChannelPnL() {
        return R.ok(costService.listChannelPnL());
    }
}
