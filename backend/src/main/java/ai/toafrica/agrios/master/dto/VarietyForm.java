package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "品种 - 创建/修改表单")
public class VarietyForm {
    @NotNull
    @Schema(description = "所属作物 ID", example = "1")
    private Long cropId;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "编码 (与 cropId 联合唯一)", example = "V-003")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "名称", example = "黑番茄")
    private String name;

    @Size(max = 255)
    @Schema(description = "特性描述")
    private String traits;

    @Min(1)
    @Schema(description = "Override of crop shelf life (days). Leave null to use crop default.", example = "10")
    private Integer shelfLifeDays;
}
