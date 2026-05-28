package ai.toafrica.agrios.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String status;
    /** Sprint 37: STAFF / PARTNER / CUSTOMER. */
    private String userType;
    /** External organization name, only meaningful for PARTNER / CUSTOMER. */
    private String orgName;
    /** For user_type=CUSTOMER, the customer.id this account represents. */
    private Long linkedCustomerId;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}
