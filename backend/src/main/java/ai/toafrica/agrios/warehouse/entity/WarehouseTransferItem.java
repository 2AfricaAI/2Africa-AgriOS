package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("warehouse_transfer_item")
public class WarehouseTransferItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transferId;
    private Long inputItemId;
    private BigDecimal qty;
    private String remark;
}
