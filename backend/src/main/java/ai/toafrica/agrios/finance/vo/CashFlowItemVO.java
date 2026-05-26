package ai.toafrica.agrios.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 现金流明细项 - Sprint 18
 *   type:
 *     order_due  应收订单到期 (inflow)
 *     promise    客户承诺还款 (inflow, 覆盖 order_due)
 *     po_due     采购订单到期 (outflow)
 */
@Data
public class CashFlowItemVO {
    private String type;
    private String ref;            // 业务编码 (订单/PO/承诺 id)
    private String name;           // 客户名 / 供应商名
    private BigDecimal amount;     // KES
    private LocalDate date;        // 预期收/付日
    private String orderCode;      // 关联订单号 (inflow 项才有)
}
