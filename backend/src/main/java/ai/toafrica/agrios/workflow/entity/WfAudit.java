package ai.toafrica.agrios.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 52 -- append-only audit log. The DB triggers added in
 * migration 051 reject UPDATE and DELETE; this entity intentionally
 * has no setter for {@code id} use other than auto-fill.
 */
@Data
@TableName("wf_audit")
public class WfAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long stepId;
    private Long actorId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String comment;
    private String ip;
    private String userAgent;
    private LocalDateTime occurredAt;
}
