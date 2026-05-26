package ai.toafrica.agrios.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Supplier - create/update form")
public class SupplierForm {

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Supplier name", example = "Athi River Fertilizers")
    private String name;

    @NotBlank
    @Pattern(regexp = "^(input_dealer|labor_contractor|utility|equipment|service|logistics|other)$",
             message = "type must be one of input_dealer/labor_contractor/utility/equipment/service/logistics/other")
    @Schema(description = "Supplier type", example = "input_dealer")
    private String type;

    @Size(max = 64)
    @Schema(description = "Tax ID / KRA PIN", example = "P051234567X")
    private String taxId;

    @Size(max = 80)
    private String contactName;

    @Size(max = 32)
    private String contactPhone;

    @Size(max = 120)
    private String contactEmail;

    @Size(max = 255)
    private String address;

    @Min(value = 0, message = "creditDays must be >= 0")
    @Schema(description = "Credit period in days. 0=COD, 7=weekly, 30=monthly", example = "30")
    private Integer creditDays;

    @Size(max = 32)
    @Schema(description = "Payment terms label", example = "Monthly")
    private String paymentTerms;

    @Schema(description = "Supplier since date", example = "2026-01-15")
    private LocalDate sinceDate;

    @Size(max = 500)
    private String remark;
}
