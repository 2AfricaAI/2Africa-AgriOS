package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "包装规格 - 创建/修改表单")
public class PackagingSpecForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "编码", example = "SP-2KG")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "名称", example = "2kg 礼盒")
    private String name;

    @NotNull
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "单件净重 (kg)", example = "2.000")
    private BigDecimal unitNetKg;

    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "单件毛重 (kg)", example = "2.150")
    private BigDecimal unitGrossKg;

    @Size(max = 64)
    @Schema(description = "材质", example = "纸盒+衬")
    private String material;
}
