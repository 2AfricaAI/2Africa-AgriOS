package ai.toafrica.agrios.packhouse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Packing - create form")
public class PackingForm {
    @NotNull
    @Schema(description = "Source batch id", example = "1")
    private Long batchId;

    @NotBlank
    @Pattern(regexp = "A|B|C", message = "grade must be A/B/C")
    @Schema(description = "Grade A/B/C", example = "A")
    private String grade;

    @NotNull
    @Schema(description = "Packaging spec id (packaging_spec)", example = "1")
    private Long specId;

    @NotNull
    @Min(1)
    @Schema(description = "Number of packages", example = "100")
    private Integer qtyUnits;

    /**
     * 净重 - 如果不传, 后端按 spec.unit_net_kg * qty_units 自动算
     */
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "Total net weight (kg) - optional, auto-calculated from spec when omitted")
    private BigDecimal netWeightKg;

    @NotNull
    @Schema(description = "Inbound location id", example = "1")
    private Long locationId;

    @NotNull
    @Schema(description = "Packing completion time", example = "2026-09-01T14:30:00")
    private LocalDateTime packedAt;

    @Size(max = 255)
    private String remark;
}
