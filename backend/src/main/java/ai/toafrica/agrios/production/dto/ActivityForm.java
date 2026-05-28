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
@Schema(description = "Activity - create/update form")
public class ActivityForm {

    @Size(max = 64)
    @Schema(description = "Client-generated idempotency key (UUID)", example = "e2c7f8...")
    private String clientUuid;

    @NotNull
    @Schema(description = "Planting plan id", example = "1")
    private Long planId;

    /**
     * 注: plot_id 不强制让前端传,后端从 plan_id 反查填入。
     * 但如果前端传了,也接受并校验 = plan.plot_id。
     */
    @Schema(description = "Plot id (optional, defaults to lookup via plan)", example = "1")
    private Long plotId;

    @NotBlank
    @Pattern(regexp = "sow|fertilize|spray|weed|water|prune|other",
             message = "activityType must be sow/fertilize/spray/weed/water/prune/other")
    @Schema(description = "Activity type", example = "sow",
            allowableValues = {"sow", "fertilize", "spray", "weed", "water", "prune", "other"})
    private String activityType;

    @NotNull
    @Schema(description = "Actual occurrence date", example = "2026-06-15")
    private LocalDate occurDate;

    @Schema(description = "Array of sys_file.id for attached photos", example = "[1, 2, 3]")
    private List<Long> photos;

    @Size(max = 64)
    @Schema(description = "GPS coordinates, e.g. -1.2864,36.8172")
    private String locationGps;

    @Size(max = 500)
    @Schema(description = "Remark")
    private String remark;

    // ====== V2.0 Phase 2 成本字段 (Sprint 11) ======

    @DecimalMin(value = "0", inclusive = true, message = "laborCost must be >= 0")
    @Schema(description = "Labor cost", example = "500")
    private BigDecimal laborCost;

    @DecimalMin(value = "0", inclusive = true, message = "waterCost must be >= 0")
    @Schema(description = "Water cost", example = "80")
    private BigDecimal waterCost;

    @DecimalMin(value = "0", inclusive = true, message = "electricityCost must be >= 0")
    @Schema(description = "Electricity cost", example = "60")
    private BigDecimal electricityCost;

    @DecimalMin(value = "0", inclusive = true, message = "fertilizerCost must be >= 0")
    @Schema(description = "Fertilizer cost", example = "200")
    private BigDecimal fertilizerCost;

    @DecimalMin(value = "0", inclusive = true, message = "otherCost must be >= 0")
    @Schema(description = "Other cost", example = "0")
    private BigDecimal otherCost;

    @Pattern(regexp = "^(KES|USD|EUR)?$", message = "costCurrency must be KES / USD / EUR")
    @Schema(description = "Cost currency", example = "KES")
    private String costCurrency;

    // ====== Sprint 17.7 - Activity ↔ PO 行 ======
    @Schema(description = "Related labor PO line (optional)")    private Long laborPoItemId;
    @Schema(description = "Related water-fee PO line (optional)")    private Long waterPoItemId;
    @Schema(description = "Related electricity PO line (optional)")    private Long electricityPoItemId;
    @Schema(description = "Related fertilizer PO line (optional)")    private Long fertilizerPoItemId;
    @Schema(description = "Related misc PO line (optional)")    private Long otherPoItemId;

    // ====== Sprint 23f - 投入品明细 (PHI 检查依赖) ======
    @Schema(description = "Input lines: which input items were used and how much")
    private List<InputLine> inputs;

    @Data
    public static class InputLine {
        @NotNull
        @Schema(description = "input_item.id", example = "4")
        private Long inputItemId;

        @NotNull
        @DecimalMin(value = "0.001", message = "qty must be > 0")
        @Schema(description = "Quantity used", example = "0.5")
        private BigDecimal qty;

        @NotBlank
        @Size(max = 16)
        @Schema(description = "Unit (kg / L / pack)", example = "L")
        private String unit;

        @DecimalMin(value = "0", inclusive = true)
        @Schema(description = "Cost (optional)", example = "200")
        private BigDecimal cost;
    }
}
