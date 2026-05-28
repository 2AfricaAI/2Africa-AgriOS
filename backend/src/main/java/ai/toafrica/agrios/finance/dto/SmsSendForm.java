package ai.toafrica.agrios.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Send SMS / WhatsApp request")
public class SmsSendForm {

    @NotNull
    private Long customerId;

    @Schema(description = "Optional: target a specific order (used for {orderCode} {amount} {dueDate} {daysOverdue} placeholders)")
    private Long orderId;

    @NotBlank
    @Schema(description = "Template code: AR_PRE_REMIND / AR_OVERDUE / AR_PROMISE_DUE")
    private String templateCode;

    @Pattern(regexp = "^(sms|whatsapp)$", message = "channel must be sms or whatsapp")
    @Schema(description = "Defaults to the template channel; can be overridden here")
    private String channel;

    @Size(max = 32)
    @Schema(description = "Override target phone (defaults to customer.contact_phone)")
    private String phoneOverride;
}
