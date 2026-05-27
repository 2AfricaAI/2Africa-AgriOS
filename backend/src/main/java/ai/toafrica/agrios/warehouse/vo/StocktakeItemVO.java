package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StocktakeItemVO {
    private Long id;
    private Long stocktakeId;
    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String unit;
    private BigDecimal systemQty;
    private BigDecimal countQty;
    private BigDecimal diffQty;
    private String remark;
}
