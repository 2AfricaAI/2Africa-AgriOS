package ai.toafrica.agrios.org.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 51 -- nested tree shape for the frontend org tree picker.
 *
 * <p>Flattening to a parent_id list is also supported by the controller
 * (returns plain {@code OrgNode} list); this VO is the convenience
 * shape for el-tree / single-fetch render.</p>
 */
@Data
public class OrgNodeTreeVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String type;
    private Long managerId;
    private String managerName;       // resolved at service layer
    private String location;
    private Integer active;
    private Integer sortNo;
    private Integer depth;

    private List<OrgNodeTreeVO> children = new ArrayList<>();
}
