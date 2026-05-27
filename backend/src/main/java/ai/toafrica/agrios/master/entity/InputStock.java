package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投入品库存快照 (Sprint 22.2)
 *
 * 每行 = 一个 input_item 在一个 warehouse 的当前余量。
 * UNIQUE(input_item_id, warehouse_id).
 */
@Data
@TableName("input_stock")
public class InputStock {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inputItemId;
    private Long warehouseId;

    /** 实际在库量 (base unit) */
    private BigDecimal qtyOnHand;

    /** 预留量 (待出库, 后续 Sprint 用) */
    private BigDecimal qtyReserved;

    /** 最后一次出入库时间 */
    private LocalDateTime lastStockAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
