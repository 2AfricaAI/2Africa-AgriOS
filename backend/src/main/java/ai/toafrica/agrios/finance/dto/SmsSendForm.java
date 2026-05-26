package ai.toafrica.agrios.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送 SMS / WhatsApp 请求")
public class SmsSendForm {

    @NotNull
    private Long customerId;

    @Schema(description = "可选: 针对具体订单 (用于 {orderCode} {amount} {dueDate} {daysOverdue} 占位符)")
    private Long orderId;

    @NotBlank
    @Schema(description = "模板编码: AR_PRE_REMIND / AR_OVERDUE / AR_PROMISE_DUE")
    private String templateCode;

    @Pattern(regexp = "^(sms|whatsapp)$", message = "channel must be sms or whatsapp")
    @Schema(description = "默认走模板的 channel; 此处可覆盖")
    private String channel;

    @Size(max = 32)
    @Schema(description = "覆盖目标手机 (默认取 customer.contact_phone)")
    private String phoneOverride;
}
