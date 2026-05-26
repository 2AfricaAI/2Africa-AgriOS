package ai.toafrica.agrios.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "农事记录 - 创建/修改表单")
public class ActivityForm {

    @Size(max = 64)
    @Schema(description = "前端生成的幂等键(UUID)", example = "e2c7f8...")
    private String clientUuid;

    @NotNull
    @Schema(description = "种植计划 ID", example = "1")
    private Long planId;

    /**
     * 注: plot_id 不强制让前端传,后端从 plan_id 反查填入。
     * 但如果前端传了,也接受并校验 = plan.plot_id。
     */
    @Schema(description = "地块 ID (可选,默认从 plan 反查)", example = "1")
    private Long plotId;

    @NotBlank
    @Pattern(regexp = "sow|fertilize|spray|weed|water|prune|other",
             message = "activityType must be sow/fertilize/spray/weed/water/prune/other")
    @Schema(description = "活动类型", example = "sow",
            allowableValues = {"sow", "fertilize", "spray", "weed", "water", "prune", "other"})
    private String activityType;

    @NotNull
    @Schema(description = "实际发生日期", example = "2026-06-15")
    private LocalDate occurDate;

    @Schema(description = "关联照片的 sys_file.id 数组", example = "[1, 2, 3]")
    private List<Long> photos;

    @Size(max = 64)
    @Schema(description = "GPS 坐标,例如 -1.2864,36.8172")
    private String locationGps;

    @Size(max = 500)
    @Schema(description = "备注")
    private String remark;

    // ====== V2.0 Phase 2 成本字段 (Sprint 11) ======

    @DecimalMin(value = "0", inclusive = true, message = "laborCost must be >= 0")
    @Schema(description = "人工成本", example = "500")
    private BigDecimal laborCost;

    @DecimalMin(value = "0", inclusive = true, message = "waterCost must be >= 0")
    @Schema(description = "水费", example = "80")
    private BigDecimal waterCost;

    @DecimalMin(value = "0", inclusive = true, message = "electricityCost must be >= 0")
    @Schema(description = "电费", example = "60")
    private BigDecimal electricityCost;

    @DecimalMin(value = "0", inclusive = true, message = "fertilizerCost must be >= 0")
    @Schema(description = "肥料成本", example = "200")
    private BigDecimal fertilizerCost;

    @DecimalMin(value = "0", inclusive = true, message = "otherCost must be >= 0")
    @Schema(description = "其他成本", example = "0")
    private BigDecimal otherCost;

    @Pattern(regexp = "^(KES|USD|EUR)?$", message = "costCurrency must be KES / USD / EUR")
    @Schema(description = "成本货币", example = "KES")
    private String costCurrency;
}
