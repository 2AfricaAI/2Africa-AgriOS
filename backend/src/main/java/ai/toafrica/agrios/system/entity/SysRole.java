package ai.toafrica.agrios.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;          // SUPER_ADMIN / MANAGER / WORKER / ...
    private String name;
    private String dataScope;     // self / group / all
    /** Sprint 36: 1 = preset role (immutable), 0 = custom role created via UI. */
    private Integer isBuiltIn;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
