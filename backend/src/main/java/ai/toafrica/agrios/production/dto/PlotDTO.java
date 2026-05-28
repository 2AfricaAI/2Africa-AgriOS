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
@Schema(description = "Plot create / edit request")
public class PlotDTO {

    @Schema(description = "Plot id (required when editing)")
    private Long id;

    @NotBlank(message = "Plot code is required")
    @Size(max = 32)
    @Schema(description = "Plot code P-NNN", example = "P-006")
    private String code;

    @NotBlank(message = "Plot name is required")
    @Size(max = 64)
    @Schema(description = "Plot name")
    private String name;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.01", message = "Area must be greater than 0")
    @Schema(description = "Area (mu)", example = "3.5")
    private BigDecimal areaMu;

    @Schema(description = "Location (lat/lng or description)")
    private String location;

    @Schema(description = "Soil type: loam/sand/clay/saline")
    private String soilType;

    @Schema(description = "Irrigation method: drip/spray/furrow")
    private String irrigation;

    @NotNull(message = "Owner is required")
    @Schema(description = "Owner staff_id")
    private Long ownerId;

    @Schema(description = "Allowed crop id array (soft constraint)")
    private List<Long> allowedCrops;

    @Schema(description = "Status active/inactive/fallow", example = "active")
    private String status = "active";

    @Size(max = 255)
    @Schema(description = "Remark")
    private String remark;
}
