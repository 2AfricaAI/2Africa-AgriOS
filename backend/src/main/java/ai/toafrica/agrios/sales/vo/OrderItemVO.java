package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Order item with SKU details for detail view")
public class OrderItemVO {
    private Long id;
    private Long orderId;

    private Long skuId;
    private String skuCode;
    private String skuName;
    private String grade;

    private String cropName;
    private String varietyName;

    private Long specId;
    private String specCode;
    private String specName;

    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal qtyShipped;

    private String remark;
}
