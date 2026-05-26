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
@Schema(description = "包装单 - 创建表单")
public class PackingForm {
    @NotNull
    @Schema(description = "源批次 ID", example = "1")
    private Long batchId;

    @NotBlank
    @Pattern(regexp = "A|B|C", message = "grade must be A/B/C")
    @Schema(description = "等级 A/B/C", example = "A")
    private String grade;

    @NotNull
    @Schema(description = "包装规格 ID (packaging_spec)", example = "1")
    private Long specId;

    @NotNull
    @Min(1)
    @Schema(description = "包装件数", example = "100")
    private Integer qtyUnits;

    /**
     * 净重 - 如果不传, 后端按 spec.unit_net_kg * qty_units 自动算
     */
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "净重总量(kg) 可选, 默认按 spec 自动算")
    private BigDecimal netWeightKg;

    @NotNull
    @Schema(description = "入库位 ID", example = "1")
    private Long locationId;

    @NotNull
    @Schema(description = "包装完成时间", example = "2026-09-01T14:30:00")
    private LocalDateTime packedAt;

    @Size(max = 255)
    private String remark;
}
