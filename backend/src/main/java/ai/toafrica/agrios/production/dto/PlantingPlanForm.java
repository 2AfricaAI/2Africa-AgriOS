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
@Schema(description = "种植计划 - 创建/修改表单")
public class PlantingPlanForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "计划编码", example = "PL-26-0001")
    private String code;

    @NotNull
    @Schema(description = "地块 ID", example = "1")
    private Long plotId;

    @NotNull
    @Schema(description = "作物 ID", example = "1")
    private Long cropId;

    @Schema(description = "品种 ID (可选)", example = "1")
    private Long varietyId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Schema(description = "种植面积 (亩)", example = "3.5")
    private BigDecimal areaMu;

    @NotNull
    @Schema(description = "计划起始日期", example = "2026-06-01")
    private LocalDate planStartDate;

    @NotNull
    @Schema(description = "计划采收日期 (须 ≥ 起始日期)", example = "2026-09-01")
    private LocalDate planHarvestDate;

    @DecimalMin(value = "0", inclusive = true)
    @Schema(description = "目标产量 (kg)", example = "5000")
    private BigDecimal targetYieldKg;

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;
}
