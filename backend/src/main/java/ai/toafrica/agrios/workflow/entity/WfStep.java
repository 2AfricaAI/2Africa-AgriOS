package ai.toafrica.agrios.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 52 -- a single approval node inside an instance.
 *
 * <p>Same {@code seq} means parallel (all rows must approve before the
 * group is complete). Different seqs are serial (lower runs first).</p>
 */
@Data
@TableName("wf_step")
public class WfStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Integer seq;
    /** 'approval' / 'cc' / 'sign' / 'pay' */
    private String type;

    private Long assigneeId;
    private String assigneeRole;
    private String assigneeLookup;

    /**
     * pending / in_progress / approved / rejected / returned / delegated /
     * skipped / expired
     */
    private String status;
    private String action;
    private Long actorId;
    private String comment;

    private Integer slaHours;
    private LocalDateTime slaDueAt;
    private Long escalatedToId;

    private LocalDateTime activatedAt;
    private LocalDateTime completedAt;
}
