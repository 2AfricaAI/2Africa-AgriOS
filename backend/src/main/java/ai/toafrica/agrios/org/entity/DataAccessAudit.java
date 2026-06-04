package ai.toafrica.agrios.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 51 (decision #5) -- append-only log of every read by a user
 * whose effective {@code data_scope='all'}. The {@code DataAccessAuditAspect}
 * (added Day 3) writes one row per query; controllers do not write to
 * this table directly.
 *
 * <p>At the application layer the table is treated as write-only beyond
 * insert. Production hardening in Sprint 60 will revoke UPDATE/DELETE
 * permissions from the application's DB user so this becomes physically
 * unmodifiable.</p>
 */
@Data
@TableName("data_access_audit")
public class DataAccessAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String roleCode;
    private String resourceType;
    /** NULL for list endpoints. */
    private Long resourceId;
    private String querySummary;
    /** List-endpoint row count. */
    private Integer rowCount;
    private String ip;
    private String userAgent;
    private LocalDateTime accessedAt;
}
