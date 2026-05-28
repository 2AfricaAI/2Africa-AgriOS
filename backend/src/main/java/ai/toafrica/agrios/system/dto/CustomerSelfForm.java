package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sprint 37: create a CUSTOMER self-service account that logs in to /portal
 * and only sees their own orders / statements / payments.
 */
@Data
@Schema(description = "Customer self-service account form")
public class CustomerSelfForm {
    @NotBlank
    private String username;

    @NotBlank
    private String nickname;

    private String phone;
    private String email;

    /** Only required on create. */
    private String password;

    @NotNull
    @Schema(description = "Which customer.id row this account represents")
    private Long linkedCustomerId;
}
