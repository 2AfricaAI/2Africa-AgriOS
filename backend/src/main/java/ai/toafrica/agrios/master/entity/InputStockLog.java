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
 * 出入库流水日志 (Sprint 22.3)
 * 审计级: 只写不改不删
 */
@Data
@TableName("input_stock_log")
public class InputStockLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inputItemId;
    private Long warehouseId;

    /** IN / OUT */
    private String direction;
    /** 数量 (正数, direction 决定加减) */
    private BigDecimal qty;

    /** po_receive / activity_consume / stocktake_adjust / damage / return_in / transfer_in / transfer_out / manual */
    private String reasonType;

    /** 多态引用: purchase_order / activity / stocktake / ... */
    private String referenceType;
    private Long referenceId;

    /** 变动后的 qty_on_hand 快照 */
    private BigDecimal qtyAfter;

    private Long operatorId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
