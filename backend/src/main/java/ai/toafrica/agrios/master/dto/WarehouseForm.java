package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "仓库/库位 - 创建/修改表单")
public class WarehouseForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "编码", example = "W03")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "名称", example = "二号包装仓")
    private String name;

    @NotBlank
    @Pattern(regexp = "normal|cold|quarantine", message = "type 必须是 normal/cold/quarantine")
    @Schema(description = "类型", example = "normal", allowableValues = {"normal", "cold", "quarantine"})
    private String type;

    @Schema(description = "父节点 ID, 0 表示顶层节点", example = "0", defaultValue = "0")
    private Long parentId;

    @DecimalMin(value = "0.00", inclusive = true)
    @Schema(description = "容量 (kg)", example = "5000")
    private BigDecimal capacityKg;
}
