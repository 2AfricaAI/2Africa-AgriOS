package ai.toafrica.agrios.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 51 -- cross-tree dimension tag. See PRD-ORG-v0.2 § 3.3 for the
 * allowed categories (SEASON / PROJECT / COMPLIANCE_ZONE / CERTIFICATION).
 *
 * <p>Strict rules enforced at service layer: tags MUST NOT nest, MUST NOT
 * encode hierarchy, MUST NOT replace node types. Cross-org dimensions
 * only.</p>
 */
@Data
@TableName("org_tag")
public class OrgTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String category;
    private Integer active;
    private String description;
    private LocalDateTime createdAt;
}
