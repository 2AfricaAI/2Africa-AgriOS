package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Sprint 36: Form used to create / rename a custom role.
 * Built-in roles cannot be created or renamed through this endpoint.
 */
@Data
@Schema(description = "Custom role create / rename form")
public class SysRoleForm {
    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$",
            message = "Code must be UPPER_SNAKE_CASE, 2-32 chars")
    @Schema(description = "Role code, UPPER_SNAKE, immutable after creation. e.g. ACCOUNTANT")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Human-readable name, e.g. Accountant")
    private String name;

    @Pattern(regexp = "^(self|group|all)$",
            message = "Data scope must be self / group / all")
    @Schema(description = "Data scope, defaults to 'self'")
    private String dataScope;

    @Size(max = 255)
    private String remark;
}
