package com.albertsfarm.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "作物 - 创建/修改表单")
public class CropForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "编码,例如 CR-005", example = "CR-005")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "名称", example = "西红柿")
    private String name;

    @Size(max = 32)
    @Schema(description = "分类: 叶菜/果蔬/根茎...", example = "果蔬")
    private String category;

    @Size(max = 8)
    @Schema(description = "计量单位", example = "kg", defaultValue = "kg")
    private String unit;

    @Min(0)
    @Schema(description = "生长周期(天)", example = "90")
    private Integer cycleDays;

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;
}
