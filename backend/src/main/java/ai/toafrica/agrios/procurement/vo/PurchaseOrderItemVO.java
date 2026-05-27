package ai.toafrica.agrios.procurement.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemVO {
    private Long id;
    private Long poId;

    /** Sprint 22.1: 软外键到 input_item 主数据 */
    private Long inputItemId;
    /** Sprint 22.1: input_item 主数据当前名称 (从 LEFT JOIN 取) */
    private String inputItemName;
    /** Sprint 22.1: input_item 主数据当前 SKU code (从 LEFT JOIN 取) */
    private String inputItemCode;

    private String inputType;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal receivedQty;
    private String remark;
}
