package ai.toafrica.agrios.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * Sprint 51 -- user-to-node membership with effective date range.
 *
 * <p>Decision #4 -- cross-farm payroll attribution is driven by
 * {@code effective_from} / {@code effective_to} on this table. Each work
 * day is associated with whichever org_user row was effective at that
 * employee on that date. {@code hr_attendance.node_id} is the
 * downstream cache (added in Sprint 54).</p>
 *
 * <p>Constraint: exactly one row per user with {@code is_primary=1} at
 * any given moment. Decision #2 -- deputy managers use
 * {@code is_manager=1} (a node may have several deputies, but only one
 * primary manager recorded on {@link OrgNode#getManagerId()}).</p>
 *
 * <p>History rule: when an employee is reassigned, the active row is
 * closed by setting its {@code effective_to} -- do NOT update in place.
 * A new row is inserted for the new node.</p>
 */
@Data
@TableName("org_user")
public class OrgUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long nodeId;

    /** Exactly one primary per user globally; service layer enforces. */
    private Integer isPrimary;

    private String position;

    /** Decision #2 -- deputy/co-manager flag (primary on org_node.manager_id). */
    private Integer isManager;

    private LocalDate effectiveFrom;
    /** NULL = still active. */
    private LocalDate effectiveTo;

    private String remark;
}
