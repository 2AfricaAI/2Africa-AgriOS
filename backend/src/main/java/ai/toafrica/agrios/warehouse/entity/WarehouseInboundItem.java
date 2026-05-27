package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("warehouse_inbound_item")
public class WarehouseInboundItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inboundId;
    private Long inputItemId;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private String remark;
}
