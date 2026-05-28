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
}
