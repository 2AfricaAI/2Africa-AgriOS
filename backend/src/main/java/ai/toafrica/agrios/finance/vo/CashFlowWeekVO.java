package ai.toafrica.agrios.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CashFlowWeekVO {
    private Integer weekIndex;          // 0 (本周) ~ 12 (第 13 周)
    private LocalDate weekStart;        // 周一
    private LocalDate weekEnd;          // 周日
    private String label;               // "Week 1 (May 25-31)"

    private BigDecimal inflow;          // 当周流入合计
    private BigDecimal outflow;         // 当周流出合计
    private BigDecimal netFlow;         // = inflow - outflow
    private BigDecimal cumulativeBalance; // 累计余额 = opening + Σnet

    private List<CashFlowItemVO> inflowItems;
    private List<CashFlowItemVO> outflowItems;
}
