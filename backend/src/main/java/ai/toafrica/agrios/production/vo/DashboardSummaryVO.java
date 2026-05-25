package ai.toafrica.agrios.production.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Dashboard 首页摘要 - 一次返回所有 KPI 和列表")
public class DashboardSummaryVO {

    // 顶部 4 个 KPI
    @Schema(description = "进行中(planned/in_progress) 计划数")
    private long activePlanCount;

    @Schema(description = "待审核农事数")
    private long pendingActivityCount;

    @Schema(description = "今日采收量 (kg)")
    private BigDecimal todayHarvestKg;

    @Schema(description = "待处理 batch 数")
    private long pendingBatchCount;

    // 趋势
    @Schema(description = "近 7 天每日采收量 [{date, qty}]")
    private List<Map<String, Object>> harvest7Days;

    @Schema(description = "按作物分组采收量 [{cropName, qty}]")
    private List<Map<String, Object>> harvestByCrop;

    // 列表
    @Schema(description = "待审核农事 Top 5")
    private List<Map<String, Object>> pendingActivities;

    @Schema(description = "最近采收 Top 5")
    private List<Map<String, Object>> recentHarvests;
}
