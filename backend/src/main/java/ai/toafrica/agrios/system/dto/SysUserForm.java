package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "User - create/update form")
public class SysUserForm {

    @NotBlank
    @Size(min = 3, max = 64)
    @Schema(description = "Login name", example = "leader_north")
    private String username;

    @Size(max = 64)
    @Schema(description = "Display name")
    private String nickname;

    @Size(max = 20)
    @Schema(description = "Phone, e.g. +254 7XX XXX XXX")
    private String phone;

    @Email
    @Size(max = 128)
    @Schema(description = "Email")
    private String email;

    /**
     * Password is REQUIRED when creating a user (server hashes with BCrypt).
     * Leave empty when updating to keep the existing password — use the
     * dedicated reset endpoint to change a password explicitly.
     */
    @Schema(description = "Plain-text password (required only on create)")
    private String password;

    @Schema(description = "Role ids to attach", example = "[2, 3]")
    private List<Long> roleIds;

    @Schema(description = "active / locked / disabled", example = "active")
    private String status;
}
