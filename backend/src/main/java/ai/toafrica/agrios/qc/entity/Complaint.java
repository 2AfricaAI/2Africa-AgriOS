package ai.toafrica.agrios.qc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer / QC complaint (Sprint 27).
 *
 * Status machine:
 *   open → investigating → resolved → closed
 *   open / investigating → escalated_to_recall  (terminal for this entity;
 *     recall_id is then populated and the recall entity owns further state)
 */
@Data
@TableName(value = "complaint", autoResultMap = true)
public class Complaint {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private LocalDateTime reportedAt;

    private Long customerId;
    private Long orderId;
    private Long batchId;
    private Long skuId;

    /** quality / quantity / late / safety / wrong_product / other */
    private String category;
    /** low / medium / high / critical */
    private String severity;
    /** phone / email / app / onsite / other */
    private String channel;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> photoIds;

    /** open / investigating / resolved / closed / escalated_to_recall */
    private String status;
    private String resolution;
    private BigDecimal resolutionAmount;

    private Long reportedById;
    private LocalDateTime resolvedAt;
    private Long resolvedById;
    private Long recallId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
