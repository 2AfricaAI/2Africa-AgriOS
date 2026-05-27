package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("warehouse_outbound_item")
public class WarehouseOutboundItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundId;
    private Long inputItemId;
    private BigDecimal requestedQty;
    private BigDecimal pickedQty;
    private BigDecimal actualQty;
    private String remark;
}
