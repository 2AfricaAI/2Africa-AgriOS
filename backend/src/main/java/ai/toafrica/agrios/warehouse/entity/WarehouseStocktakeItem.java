package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("warehouse_stocktake_item")
public class WarehouseStocktakeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stocktakeId;
    private Long inputItemId;
    private BigDecimal systemQty;
    private BigDecimal countQty;
    private BigDecimal diffQty;
    private String remark;
}
