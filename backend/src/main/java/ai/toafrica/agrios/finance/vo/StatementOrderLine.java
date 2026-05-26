package ai.toafrica.agrios.finance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StatementOrderLine {
    private Long orderId;
    private String orderCode;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstanding;
    private String currency;
    private String status;
    private String paymentStatus;
}
