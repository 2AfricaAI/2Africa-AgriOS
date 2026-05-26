package ai.toafrica.agrios.procurement.dto;

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
@Schema(description = "Purchase Order - create / edit form (header + items)")
public class PurchaseOrderForm {

    @NotNull
    private Long supplierId;

    @NotNull
    private LocalDate orderDate;

    @Schema(description = "Expected delivery date")
    private LocalDate expectedDate;

    @NotBlank
    @Pattern(regexp = "^(KES|USD|EUR)$")
    @Schema(description = "Order currency", example = "KES")
    private String currency;

    @Schema(description = "FX rate to KES (default 1.0)")
    private BigDecimal fxRate;

    @Size(max = 500)
    private String remark;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<Item> items;

    @Data
    @Schema(description = "Purchase order line item")
    public static class Item {
        @NotBlank
        @Pattern(regexp = "^(labor|water|electricity|fertilizer|seed|pesticide|equipment|service|other)$",
                 message = "inputType must be one of labor/water/electricity/fertilizer/seed/pesticide/equipment/service/other")
        @Schema(description = "Input type", example = "fertilizer")
        private String inputType;

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Description, e.g. NPK 17:17:17 50kg bag")
        private String description;

        @NotNull
        @DecimalMin(value = "0.001", inclusive = true, message = "quantity must be > 0")
        private BigDecimal quantity;

        @NotBlank
        @Size(max = 16)
        @Schema(description = "Unit: bag / kg / L / hour / person-day / lump-sum")
        private String unit;

        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        private BigDecimal unitPrice;

        @Size(max = 255)
        private String remark;
    }
}
