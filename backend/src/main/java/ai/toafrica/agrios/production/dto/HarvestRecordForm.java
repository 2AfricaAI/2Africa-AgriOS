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
@Schema(description = "Harvest record - create form")
public class HarvestRecordForm {

    @Size(max = 64)
    @Schema(description = "Client-generated idempotency key (UUID)")
    private String clientUuid;

    @NotNull
    @Schema(description = "Planting plan id", example = "1")
    private Long planId;

    /** crop/variety/plot 自动从 plan 反查,前端可不传 */
    @Schema(description = "(Optional) variety id, defaults to plan.variety_id")
    private Long varietyId;

    @NotNull
    @Schema(description = "Harvest date", example = "2026-09-01")
    private LocalDate harvestDate;

    @NotNull
    @DecimalMin(value = "0.001", inclusive = true)
    @Schema(description = "Harvest qty (kg)", example = "1250.500")
    private BigDecimal qtyKg;

    @Size(max = 64)
    @Schema(description = "GPS coordinates, e.g. -1.2864,36.8172 (Sprint 20.5 mobile)")
    private String locationGps;

    @Schema(description = "Photo sys_file.id array")
    private List<Long> photos;

    @Size(max = 255)
    private String remark;
}
