package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

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

    @Size(max = 128)
    private String spec;

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

    @Pattern(regexp = "^(active|inactive)?$")
    private String status;

    @Size(max = 255)
    private String remark;
}
