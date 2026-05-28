package ai.toafrica.agrios.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Payment receipt entry")
public class PaymentForm {
    @NotNull
    private Long orderId;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    @Pattern(regexp = "^(KES|USD|EUR)$", message = "currency must be KES / USD / EUR")
    private String currency;

    @Schema(description = "FX rate to KES (default 1.0 if currency = KES)")
    private BigDecimal fxRate;

    @NotNull
    @Pattern(regexp = "^(cash|bank|cheque|loop_online|loop_pos)$",
             message = "method must be cash / bank / cheque / loop_online / loop_pos")
    private String method;

    @NotNull
    private LocalDate paymentDate;

    @Size(max = 64)
    private String referenceNo;

    @Size(max = 64)
    @Schema(description = "POS terminal id (set when method = loop_pos)")
    private String posTerminalId;

    @Size(max = 32)
    @Schema(description = "Loop internal channel: mpesa / card / bank (usually written by Loop webhook)")
    private String channel;

    @Size(max = 255)
    private String remark;
}
