package ai.toafrica.agrios.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收入流水 - V2.0 Phase 2 P&L 事实表.
 * 每次发货完成时,按 OrderItem 粒度落一条.
 */
@Data
@TableName("revenue")
public class Revenue {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long orderItemId;
    private Long fulfillmentId;
    private Long skuId;
    private Long customerId;
    private Long batchId;

    private BigDecimal qty;
    private BigDecimal grossAmount;
    private BigDecimal tax;
    private BigDecimal netAmount;
    private String currency;

    private LocalDate recognitionDate;

    /** recognized / reversed / adjusted */
    private String status;

    /** b2b / retail / export / online */
    private String channel;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
}
