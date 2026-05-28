package ai.toafrica.agrios.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Recall trigger form")
public class RecallForm {

    @NotNull
    @Schema(description = "Batch id to recall")
    private Long batchId;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Reason for the recall (free text, will appear on the PDF report)")
    private String reason;

    @Schema(description = "batch_only / batch_plus_children. Defaults to batch_only.",
            example = "batch_only")
    private String scope;

    @Schema(description = "Source complaint id, NULL if this is a QC-initiated recall")
    private Long sourceComplaintId;
}
