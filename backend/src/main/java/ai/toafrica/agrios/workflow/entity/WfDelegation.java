package ai.toafrica.agrios.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sprint 52 -- temporary authority transfer for approvals.
 *
 * <p>{@code scopeModules} is a CSV ('hr,admin') or NULL (= all modules).
 * Active row must satisfy {@code from_date <= today <= to_date}.</p>
 */
@Data
@TableName("wf_delegation")
public class WfDelegation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long delegatorId;
    private Long delegateeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String scopeModules;
    private String reason;
    private Integer active;

    private LocalDateTime createdAt;
    private Long createdBy;
}
