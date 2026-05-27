package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Input item master data - create / update form")
public class InputItemForm {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Code, e.g. II-0007", example = "II-0007")
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 128)
    private String nameEn;

    @NotBlank
    @Pattern(regexp = "seed|fertilizer|pesticide|construction|spare_parts|tools|packaging|other",
             message = "inputType must be one of: seed/fertilizer/pesticide/construction/spare_parts/tools/packaging/other")
    private String inputType;

    @Size(max = 64)
    @Schema(description = "L2 sub-category (e.g., nitrogen/phosphate/compound/organic)")
    private String categoryL2;

    @Size(max = 128)
    private String spec;

    @DecimalMin(value = "0", inclusive = false, message = "packQty must be > 0")
    @Schema(description = "Quantity per pack (e.g., 50 for 50kg/bag)")
    private BigDecimal packQty;

    @Size(max = 32)
    @Schema(description = "Pack label (bag/bottle/box/can/sack)")
    private String packUnitLabel;

    @NotBlank
    @Size(max = 16)
    private String unit;

    @Size(max = 128)
    private String activeIngredient;

    @Size(max = 64)
    private String registrationNo;

    @Min(value = 0, message = "phiDays must be >= 0")
    @Schema(description = "Pre-Harvest Interval days (pesticide only)", example = "14")
    private Integer phiDays;

    @Schema(description = "Default supplier id (optional)")
    private Long defaultSupplierId;

    @Schema(description = "Default warehouse id for receiving (optional)")
    private Long defaultWarehouseId;

    @DecimalMin(value = "0", inclusive = true, message = "minStockQty must be >= 0")
    @Schema(description = "Low-stock alert threshold (base unit)")
    private BigDecimal minStockQty;

    @Pattern(regexp = "^(active|inactive)?$")
    private String status;

    @Size(max = 255)
    private String remark;
}
