package ai.toafrica.agrios.procurement.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemVO {
    private Long id;
    private Long poId;
    private String inputType;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal receivedQty;
    private String remark;
}
