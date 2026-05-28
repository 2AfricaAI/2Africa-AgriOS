package ai.toafrica.agrios.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 37: create / update a PARTNER user (external agronomist, GAP auditor,
 * bank officer, landlord, insurance adjuster).
 */
@Data
@Schema(description = "External partner user form")
public class PartnerForm {
    @NotBlank
    private String username;

    @NotBlank
    private String nickname;

    private String phone;
    private String email;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "External org name, e.g. KEPHIS, Equity Bank")
    private String orgName;

    /** Only required on create. */
    private String password;

    @NotEmpty
    @Schema(description = "One or more partner subtype codes (AGRONOMIST / GAP_AUDITOR / ...)")
    private List<String> subtypes;

    @Schema(description = "Per-user scope rows: PLOT/CUSTOMER ids + optional date window")
    private List<ScopeRow> scopes;

    /** Optional - explicit role IDs override the default subtype-to-role map. */
    private List<Long> roleIds;

    @Data
    public static class ScopeRow {
        /** PLOT / CUSTOMER / WAREHOUSE / DATE_WINDOW / ALL */
        @NotBlank
        private String scopeType;
        private Long scopeId;
        private LocalDate validFrom;
        private LocalDate validTo;
    }
}
