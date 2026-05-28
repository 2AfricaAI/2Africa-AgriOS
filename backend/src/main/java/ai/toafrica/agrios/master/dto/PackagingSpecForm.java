package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Packaging spec - create/update form")
public class PackagingSpecForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "Code", example = "SP-2KG")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Name", example = "2kg gift box")
    private String name;

    @NotNull
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "Per-unit net weight (kg)", example = "2.000")
    private BigDecimal unitNetKg;

    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "Per-unit gross weight (kg)", example = "2.150")
    private BigDecimal unitGrossKg;

    @Size(max = 64)
    @Schema(description = "Material", example = "Cardboard + liner")
    private String material;
}
