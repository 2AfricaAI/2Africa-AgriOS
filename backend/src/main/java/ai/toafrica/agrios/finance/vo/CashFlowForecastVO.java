package ai.toafrica.agrios.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 13 周滚动现金流预测 - Sprint 18.1.
 *   未来 13 周, 每周净流入/累计余额.
 *   数据来源: AR due_date + 客户承诺还款 + AP due_date.
 */
@Data
@Schema(description = "13-Week Rolling Cash Flow Forecast")
public class CashFlowForecastVO {
    private BigDecimal openingBalance;
    private LocalDate generatedAt;
    private LocalDate horizonEnd;

    private List<CashFlowWeekVO> weeks;

    // ---- 风险指标 ----
    private BigDecimal minBalance;
    private Integer minBalanceWeek;        // 第几周降到最低
    private LocalDate minBalanceDate;
    private boolean gapAlert;              // 任何周累计 < 0
    private BigDecimal totalInflow13w;
    private BigDecimal totalOutflow13w;
    private BigDecimal netFlow13w;
    private BigDecimal endingBalance;      // 13 周末余额
}
