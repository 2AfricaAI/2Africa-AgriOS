package ai.toafrica.agrios.packhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_adjust_log")
public class InventoryAdjustLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inventoryId;
    private String adjustType;       // in/out/lock/unlock/loss/audit
    private String reasonCode;
    private BigDecimal qtyBefore;
    private BigDecimal qtyChange;
    private BigDecimal qtyAfter;
    private String fieldName;        // qty_avail / qty_locked / ...
    private String refType;          // order/fulfillment/packing/manual
    private Long refId;
    private String remark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
