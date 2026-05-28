package ai.toafrica.agrios.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sprint 37: per-user data scope.
 *
 *   scope_type = 'PLOT' / 'CUSTOMER' / 'WAREHOUSE' / 'DATE_WINDOW' / 'ALL'
 *   scope_id   = id of plot / customer / warehouse, or NULL for DATE_WINDOW + ALL
 *
 * DATE_WINDOW rows carry valid_from + valid_to instead of an id and are AND-ed
 * with any other scope rows (so an INSURANCE adjuster can be limited to
 * plot 17 AND 2026-04-01 to 2026-07-31).
 */
@Data
@TableName("sys_user_scope")
public class SysUserScope {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String scopeType;
    private Long scopeId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
}
