package ai.toafrica.agrios.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Sales Order - create / edit form (header + items)")
public class SalesOrderForm {

    @NotNull
    @Schema(description = "Customer id")
    private Long customerId;

    @NotNull
    @Schema(description = "Order date", example = "2026-05-25")
    private LocalDate orderDate;

    @NotNull
    @Schema(description = "Expected delivery date", example = "2026-05-30")
    private LocalDate deliveryDate;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Ship-to address", example = "Westlands, Nairobi")
    private String shipTo;

    @NotBlank
    @Pattern(regexp = "^(KES|USD|EUR)$", message = "currency must be KES / USD / EUR")
    @Schema(description = "Order currency", example = "KES", defaultValue = "KES")
    private String currency;

    @Size(max = 500)
    @Schema(description = "Remark")
    private String remark;

    @NotEmpty(message = "At least one item is required")
    @Valid
    @Schema(description = "Line items")
    private List<Item> items;

    @Data
    @Schema(description = "Order line item")
    public static class Item {
        @NotNull
        @Schema(description = "SKU id")
        private Long skuId;

        @NotNull
        @DecimalMin(value = "0.001", inclusive = true, message = "qty must be > 0")
        @Schema(description = "Quantity (units)")
        private BigDecimal qty;

        @NotNull
        @DecimalMin(value = "0", inclusive = true, message = "unitPrice must be >= 0")
        @Schema(description = "Unit price in order currency")
        private BigDecimal unitPrice;

        @Size(max = 255)
        @Schema(description = "Remark")
        private String remark;
    }
}
