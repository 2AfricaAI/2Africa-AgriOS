package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginDTO {
    @NotBlank
    @Schema(description = "Username", example = "admin")
    private String username;

    @NotBlank
    @Schema(description = "Password", example = "Admin@123456")
    private String password;
}
