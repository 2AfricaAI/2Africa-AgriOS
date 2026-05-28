package ai.toafrica.agrios.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Planting plan - create/update form")
public class PlantingPlanForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "Plan code", example = "PL-26-0001")
    private String code;

    @NotNull
    @Schema(description = "Plot id", example = "1")
    private Long plotId;

    @NotNull
    @Schema(description = "Crop id", example = "1")
    private Long cropId;

    @Schema(description = "Variety id (optional)", example = "1")
    private Long varietyId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Schema(description = "Planting area (mu)", example = "3.5")
    private BigDecimal areaMu;

    @NotNull
    @Schema(description = "Plan start date", example = "2026-06-01")
    private LocalDate planStartDate;

    @NotNull
    @Schema(description = "Planned harvest date (must be ≥ start date)", example = "2026-09-01")
    private LocalDate planHarvestDate;

    @DecimalMin(value = "0", inclusive = true)
    @Schema(description = "Target yield (kg)", example = "5000")
    private BigDecimal targetYieldKg;

    @Size(max = 255)
    @Schema(description = "Remark")
    private String remark;
}
