package ai.toafrica.agrios.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Ship form - mark fulfillment as shipped")
public class ShipForm {

    @Pattern(regexp = "^(self|logistics)?$", message = "shipMethod must be self / logistics or empty")
    @Schema(description = "self / logistics", example = "self")
    private String shipMethod;

    @Size(max = 64)
    @Schema(description = "Tracking number / waybill no", example = "AWB-20260525-001")
    private String trackNo;

    @Size(max = 64)
    @Schema(description = "Driver name", example = "James Kamau")
    private String driverName;

    @Size(max = 20)
    @Schema(description = "Driver phone", example = "+254 7XX XXX XXX")
    private String driverPhone;

    @Size(max = 32)
    @Schema(description = "Vehicle plate", example = "KCA 123A")
    private String vehicleNo;

    @Size(max = 255)
    @Schema(description = "Remark")
    private String remark;
}
