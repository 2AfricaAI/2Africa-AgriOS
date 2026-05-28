package ai.toafrica.agrios.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Variety - create/update form")
public class VarietyForm {
    @NotNull
    @Schema(description = "Parent crop id", example = "1")
    private Long cropId;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Code (unique combined with cropId)", example = "V-003")
    private String code;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Name", example = "Black tomato")
    private String name;

    @Size(max = 255)
 