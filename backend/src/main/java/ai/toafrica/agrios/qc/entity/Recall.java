package ai.toafrica.agrios.qc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Batch recall (Sprint 27).
 * Status machine:
 *   initiated → quarantined → customers_notified → closed
 */
@Data
@TableName("recall")
public class Recall {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private LocalDateTime triggeredAt;
    private Long sourceComplaintId;
    private Long batchId;

    /** batch_only / batch_plus_children */
    private String scope;
    private String reason;
    /** initiated / quarantined / customers_notified / closed */
    private String status;

    private Integer affectedOrderCount;
    private Integer affectedCustomerCount;
    private BigDecimal affectedQty;

    private Long initiatedById;
    private LocalDateTime closedAt;
    private Long closedById;
    private String closedRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
