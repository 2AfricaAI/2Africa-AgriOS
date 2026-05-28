package ai.toafrica.agrios.production.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Dashboard home summary - all KPIs and lists in one call")
public class DashboardSummaryVO {

    // 顶部 4 个 KPI
    @Schema(description = "Active (planned/in_progress) plan count")
    private long activePlanCount;

    @Schema(description = "Activities awaiting review count")
    private long pendingActivityCount;

    @Schema(description = "Today's harvest qty (kg)")
    private BigDecimal todayHarvestKg;

    @Schema(description = "Pending batch count")
    private long pendingBatchCount;

    // 趋势
    @Schema(description = "Daily harvest qty over the last 7 days [{date, qty}]")
    private List<Map<String, Object>> harvest7Days;

    @Schema(description = "Harvest qty grouped by crop [{cropName, qty}]")
    private List<Map<String, Object>> harvestByCrop;

    // 列表
    @Schema(description = "Top 5 activities awaiting review")
    private List<Map<String, Object>> pendingActivities;

    @Schema(description = "Top 5 recent harvests")
    private List<Map<String, Object>> recentHarvests;
}
