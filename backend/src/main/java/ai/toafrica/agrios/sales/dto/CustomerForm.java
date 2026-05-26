package ai.toafrica.agrios.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Customer - create/update form")
public class CustomerForm {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Customer name", example = "Acme Supermarket Ltd")
    private String name;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Customer type: supermarket / restaurant / ecommerce / wholesale / export / other",
            example = "supermarket")
    private String type;

    @Size(max = 64)
    @Schema(description = "Contact person", example = "John Mwangi")
    private String contactName;

    @Size(max = 20)
    @Schema(description = "Phone", example = "+254 7XX XXX XXX")
    private String contactPhone;

    @Pattern(regexp = "^[ABCD]?$", message = "creditLevel must be A/B/C/D or empty")
    @Size(max = 8)
    @Schema(description = "Credit level A/B/C/D", example = "A")
    private String creditLevel;

    @Min(value = 0, message = "creditDays must be >= 0")
    @Schema(description = "Credit period in days. 0=COD, 7=weekly, 30=monthly", example = "30")
    private Integer creditDays;

    @Size(max = 32)
    @Schema(description = "Payment terms label (UI only): COD / Weekly / Monthly / Net 30", example = "Monthly")
    private String paymentTerms;

    @Schema(description = "Customer since date", example = "2026-01-15")
    private LocalDate sinceDate;

    @Size(max = 255)
    @Schema(description = "Remark")
    private String remark;
}
