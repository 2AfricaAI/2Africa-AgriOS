package ai.toafrica.agrios.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "地块创建 / 编辑请求")
public class PlotDTO {

    @Schema(description = "地块 ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "Plot code is required")
    @Size(max = 32)
    @Schema(description = "地块编号 P-NNN", example = "P-006")
    private String code;

    @NotBlank(message = "Plot name is required")
    @Size(max = 64)
    @Schema(description = "地块名称")
    private String name;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.01", message = "Area must be greater than 0")
    @Schema(description = "面积（亩）", example = "3.5")
    private BigDecimal areaMu;

    @Schema(description = "地理位置（经纬度或描述）")
    private String location;

    @Schema(description = "土壤类型 loam/sand/clay/saline")
    private String soilType;

    @Schema(description = "灌溉方式 drip/spray/furrow")
    private String irrigation;

    @NotNull(message = "Owner is required")
    @Schema(description = "负责人 staff_id")
    private Long ownerId;

    @Schema(description = "可种植作物 ID 数组（软约束）")
    private List<Long> allowedCrops;

    @Schema(description = "状态 active/inactive/fallow", example = "active")
    private String status = "active";

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;
}
