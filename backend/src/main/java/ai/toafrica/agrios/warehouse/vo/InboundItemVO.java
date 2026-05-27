package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InboundItemVO {
    private Long id;
    private Long inboundId;
    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String unit;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private String remark;
}
