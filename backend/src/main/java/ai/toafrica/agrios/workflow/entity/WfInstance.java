package ai.toafrica.agrios.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sprint 52 -- a live workflow instance attached to a business row.
 *
 * <p>{@code bizTable + bizId} is the reverse pointer to the underlying
 * row (hr_leave_request / admin_expense / etc). No DB FK -- intentionally
 * loose coupling so the engine can host arbitrary modules.</p>
 */
@Data
@TableName("wf_instance")
public class WfInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long definitionId;
    private String bizTable;
    private Long bizId;

    private String title;
    /** 'pending' / 'approved' / 'rejected' / 'cancelled' */
    private String status;
    private Long initiatorId;
    private Integer currentStepSeq;

    private BigDecimal amountHint;
    /** 'normal' / 'urgent' / 'critical' */
    private String urgency;
    /** Sprint 51 org node where the request originates (for assignee.lookup). */
    private Long nodeId;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String lastAction;
}
