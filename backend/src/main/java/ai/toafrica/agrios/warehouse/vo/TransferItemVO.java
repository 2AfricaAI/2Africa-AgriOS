package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferItemVO {
    private Long id;
    private Long transferId;
    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String unit;
    private BigDecimal qty;
    private String remark;
}
