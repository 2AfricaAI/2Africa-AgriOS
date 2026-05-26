package ai.toafrica.agrios.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StatementPaymentLine {
    private Long paymentId;
    private String paymentCode;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String currency;
    private BigDecimal amountKes;
    private String method;
    private String referenceNo;
    private Long orderId;
    private String orderCode;
}
