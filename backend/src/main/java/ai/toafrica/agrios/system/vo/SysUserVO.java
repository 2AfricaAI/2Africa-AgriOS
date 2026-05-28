package ai.toafrica.agrios.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String status;
    /** Sprint 37: STAFF / PARTNER / CUSTOMER. */
    private String userType;
    private String orgName;
    private Long linkedCustomerId;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Attached roles (id + code + name). */
    private List<RoleBrief> roles;

    /** Sprint 37: partner subtype codes (PARTNER user_type only). */
    private List<String> partnerSubtypes;

    @Data
    public static class RoleBrief {
        private Long id;
        private String code;
        private String name;
    }
}
