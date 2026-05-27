package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OutboundItemVO {
    private Long id;
    private Long outboundId;
    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String unit;
    private BigDecimal requestedQty;
    private BigDecimal pickedQty;
    private BigDecimal actualQty;
    private String remark;
}
