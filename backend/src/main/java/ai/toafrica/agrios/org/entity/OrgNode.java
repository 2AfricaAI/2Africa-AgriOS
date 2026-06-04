package ai.toafrica.agrios.org.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 51 -- main organization tree node.
 *
 * <p>Schema: see {@code migrations/050_org_model.sql}. Decision history
 * in {@code docs/PRD-ORG-v0.2.md}.</p>
 *
 * <p>Key constraints enforced at service layer (not at DB):</p>
 * <ul>
 *   <li>Decision #3 -- physical nodes (FARM/PACKHOUSE/PROCESSING/WAREHOUSE)
 *       can only be deactivated (active=0), never deleted</li>
 *   <li>4 physical types may not nest inside each other</li>
 *   <li>GROUP must be root and singleton</li>
 *   <li>TEAM may not have children</li>
 *   <li>One user has exactly one is_primary org_user row</li>
 * </ul>
 */
@Data
@TableName("org_node")
public class OrgNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String code;
    private String name;
    /** GROUP / FARM / PACKHOUSE / PROCESSING / WAREHOUSE / DEPT / TEAM / PROJECT */
    private String type;

    private String costCenter;
    /** Decision #2 -- single primary manager; deputies use org_user.is_manager=1. */
    private Long managerId;

    /** Path like '1/3/7' for fast subtree queries via LIKE. */
    private String ancestors;
    private Integer depth;
    private Integer sortNo;

    /** Decision #3 -- physical nodes can only flip this, not be deleted. */
    private Integer active;

    /** Free-form physical location, e.g. 'Isinya, Kajiado'. */
    private String location;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}
