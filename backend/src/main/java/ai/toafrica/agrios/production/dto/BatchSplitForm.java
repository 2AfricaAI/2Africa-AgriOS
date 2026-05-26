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
@Schema(description = "批次拆分 - 表单")
public class BatchSplitForm {

    @NotEmpty(message = "At least one child batch is required")
    @Valid
    @Schema(description = "子批次列表(每个含拆出的 kg + 备注)")
    private List<Child> children;

    @Data
    @Schema(description = "拆出的子批次")
    public static class Child {
        @NotNull
        @DecimalMin(value = "0.001", inclusive = true, message = "qtyKg must be > 0")
        @Schema(description = "拆出的 kg")
        private BigDecimal qtyKg;

        @Size(max = 255)
        @Schema(description = "备注")
        private String remark;
    }
}
