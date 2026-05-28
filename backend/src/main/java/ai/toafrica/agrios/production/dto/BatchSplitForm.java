package ai.toafrica.agrios.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Batch split - form")
public class BatchSplitForm {

    @NotEmpty(message = "At least one child batch is required")
    @Valid
    @Schema(description = "Children list (each with split kg + remark)")
    private List<Child> children;

    @Data
    @Schema(description = "Split-off children")
    public static class Child {
        @NotNull
        @DecimalMin(value = "0.001", inclusive = true, message = "qtyKg must be > 0")
        @Schema(description = "Split-off kg")
        private BigDecimal qtyKg;

        @Size(max = 255)
        @Schema(description = "Remark")
        private String remark;
    }
}
