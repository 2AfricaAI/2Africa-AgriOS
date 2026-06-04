package ai.toafrica.agrios.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sprint 51 -- many-to-many attachment of tags to nodes. Pure join table,
 * no surrogate id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("org_node_tag")
public class OrgNodeTag {
    private Long nodeId;
    private Long tagId;
    private LocalDateTime createdAt;
}
