package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("warehouse_scrap_item")
public class WarehouseScrapItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scrapId;
    private Long inputItemId;
    private BigDecimal qty;
    private String reason;
}
