package ai.toafrica.agrios.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "采收记录 - 创建表单")
public class HarvestRecordForm {

    @Size(max = 64)
    @Schema(description = "前端生成的幂等键 UUID")
    private String clientUuid;

    @NotNull
    @Schema(description = "种植计划 ID", example = "1")
    private Long planId;

    /** crop/variety/plot 自动从 plan 反查,前端可不传 */
    @Schema(description = "(可选) 品种 ID, 默认取 plan.variety_id")
    private Long varietyId;

    @NotNull
    @Schema(description = "采收日期", example = "2026-09-01")
    private LocalDate harvestDate;

    @NotNull
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "采收量 (kg)", example = "1250.500")
    private BigDecimal qtyKg;

    @Size(max = 64)
    @Schema(description = "GPS 坐标,例如 -1.2864,36.8172 (Sprint 20.5 移动端)")
    private String locationGps;

    @Schema(description = "照片 sys_file.id 数组")
    private List<Long> photos;

    @S