package ai.toafrica.agrios.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 种植计划 P&L - V2.0 Phase 2 核心报表数据.
 *
 * 收入来自 revenue 表 (Sprint 9.4 自动生成)
 * 成本来自 activity 的 5 个 bucket + activity_input.cost (Sprint 11)
 * 毛利 / 毛利率 在 service 层算
 */
@Data
@Schema(description = "Plan-level P&L breakdown")
public class PlanPnLVO {
    private Long planId;
    private String planCode;

    // === 成本明细 ===
    private BigDecimal laborCost;
    private BigDecimal waterCost;
    private BigDecimal electricityCost;
    private BigDecimal fertilizerCost;
    private BigDecimal otherCost;
    /** activity_input.cost SUM (投入品物料成本) */
    private BigDecimal inputCost;
    /** 上面所有 cost bucket 之和 */
    private BigDecimal totalCost;

    // === 收入 ===
    /** SUM(revenue.net_amount) for batches belonging to this plan */
    private BigDecimal totalRevenue;

    // === 毛利 ===
    /** revenue - cost */
    private BigDecimal grossProfit;
    /** (revenue - cost) / revenue * 100, 保留 2 位; revenue=0 时返回 null */
    private BigDecimal grossMarginPct;

    /** 当前 MVP 假设单一货币(取自最新 activity / revenue) */
    private String currency;

    /** 调试用 - 该 plan 有多少 activity */
    private Integer activityCount;
}
