package ai.toafrica.agrios.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Complaint create/update form")
public class ComplaintForm {

    @NotNull
    @Schema(description = "Time the complaint was reported (must be provided by client)", example = "2026-05-28T14:30:00")
    private LocalDateTime reportedAt;

    @Schema(description = "Customer id. NULL = internal QC complaint")
    private Long customerId;

    @Schema(description = "Related sales order id (optional)")
    private Long orderId;

    @Schema(description = "Related batch id (recommended for traceability)")
    private Long batchId;

    @Schema(description = "Related SKU id (optional)")
    private Long skuId;

    @NotBlank
    @Schema(description = "quality / quantity / late / safety / wrong_product / other", example = "quality")
    private String category;

    @NotBlank
    @Schema(description = "low / medium / high / critical", example = "high")
    private String severity;

    @NotBlank
    @Schema(description = "phone / email / app / onsite / other", example = "phone")
    private String channel;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "What the customer/inspector reported")
    private String description;

    @Schema(description = "Evidence photo file ids")
    private List<Long> photoIds;

    @Schema(description = "Resolution narrative (only set when moving to resolved)")
    private String resolution;

    @Schema(description = "Refund / credit amount in KES (optional)")
    private BigDecimal resolutionAmount;
}
