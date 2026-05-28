package ai.toafrica.agrios.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node for the role-menu assignment UI (Sprint 35).
 * Mirrors sys_menu rows but nests children so the frontend el-tree
 * can render the full hierarchy in one call.
 */
@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String type;       // dir / menu / button
    private String path;
    private String perms;
    private String icon;
    private Integer sort;
    private List<MenuTreeVO> children = new ArrayList<>();
}
