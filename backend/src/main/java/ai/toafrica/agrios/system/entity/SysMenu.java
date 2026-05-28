package ai.toafrica.agrios.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Menu / button definition (Sprint 35).
 *
 *   type = 'dir'    -> first-level container (no path)
 *   type = 'menu'   -> leaf menu (has path + perms)
 *   type = 'button' -> action under a menu (has perms only)
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private LocalDateTime createdAt;
}
