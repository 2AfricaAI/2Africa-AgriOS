package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
@Schema(description = "Login response")
public class LoginVO {
    private Long userId;
    private String username;
    private String nickname;
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;   // 秒
    private Set<String> roles;
    private Set<String> permissions;
    /** Sprint 37: STAFF / PARTNER / CUSTOMER. Frontend uses this to pick landing route. */
    private String userType;
    /** Where the SPA should route this user after login. */
    private String landingPath;
    /** Sprint 37: for CUSTOMER user_type, the customer.id this account is bound to. */
    private Long linkedCustomerId;
}
