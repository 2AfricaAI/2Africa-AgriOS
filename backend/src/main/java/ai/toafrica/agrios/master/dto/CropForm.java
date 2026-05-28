package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Crop - create/update form")
public class CropForm {
    @NotBlank
    @Size(max = 32)
    @Schema(description = "Code, e.g. CR-005", example = "CR-005")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Name", example = "Tomato")
    private String name;

    @Size(max = 32)
    @Schema(description = "Category: leafy / fruit / root ...", example = "Fruit")
    private String category;

    @Size(max = 8)
    @Schema(description = "Unit of measure", example = "kg", defaultValue = "kg")
    private String unit;

    @Min(0)
    @Schema(description = "Growth cycle (days)", example = "90")
    private Integer cycleDays;

    @Min(1)
    @Schema(description = "Default shelf life (days) - drives FEFO expiry. Variety may override.", example = "7")
    private Integer shelfLifeDays;

    @Size(max = 255)
    @Schema(description = "Remark")
    private String remark;
}
