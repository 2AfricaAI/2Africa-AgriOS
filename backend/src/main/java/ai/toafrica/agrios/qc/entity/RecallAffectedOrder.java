package ai.toafrica.agrios.qc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recall_affected_order")
public class RecallAffectedOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recallId;
    private Long orderId;
    private String orderCode;
    private Long customerId;
    private String customerName;
    private BigDecimal qty;
    private String unit;
    private LocalDateTime deliveredAt;
    private LocalDateTime notifiedAt;
    private Long notifiedById;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
