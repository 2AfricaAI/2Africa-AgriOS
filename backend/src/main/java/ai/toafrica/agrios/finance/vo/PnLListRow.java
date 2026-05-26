package ai.toafrica.agrios.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 通用 P&L 列表行 - 用于 Sprint 13 报表页面的 plan/plot/sku 3 个 tab.
 *   refType = plan / plot / sku
 *   refCode 是业务编码 (展示用)
 *   refName 是名称/标题 (展示用)
 */
@Data
@Schema(description = "Generic P&L row for list views")
public class PnLListRow {
    private Long refId;
    private String refType;
    private String refCode;
    private String refName;

    private BigDecimal totalCost;
    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
    /** 毛利率 (%, 保留 2 位); revenue=0 时为 null */
    private BigDecimal grossMarginPct;

    private String currency;

    /** 维度额外信息: plan→cropName, plot→typeOrZone, sku→grade */
    private String dimInfo;
}
