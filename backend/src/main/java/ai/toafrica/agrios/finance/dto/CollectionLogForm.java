package ai.toafrica.agrios.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "催收跟催记录录入")
public class CollectionLogForm {
    @NotNull
    private Long customerId;

    @Schema(description = "可选: 针对具体订单跟催")
    private Long orderId;

    @NotNull
    private LocalDate logDate;

    @NotNull
    @Pattern(regexp = "^(phone|whatsapp|sms|email|visit|other)$",
             message = "channel must be phone / whatsapp / sms / email / visit / other")
    private String channel;

    @Size(max = 80)
    private String contactPerson;

    @NotNull
    @Pattern(regexp = "^(promised|refused|no_answer|disputed|paid|other)$",
             message = "outcome must be promised / refused / no_answer / disputed / paid / other")
    private String outcome;

    @Schema(description = "客户承诺还款日 (outcome=promised 必填)")
    private LocalDate promisedDate;

    private BigDecimal promisedAmount;

    @Size(max = 2000)
    private String content;

    private LocalDate nextActionDate;
}
