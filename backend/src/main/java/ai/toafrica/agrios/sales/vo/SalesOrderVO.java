package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Sales Order list row (header + customer join)")
public class SalesOrderVO {
    private Long id;
    private String code;

    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerType;

    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String shipTo;

    private String currency;
    private BigDecimal totalAmount;

    private String status;

    /** unpaid / partial / paid */
    private String paymentStatus;
    private BigDecimal paidAmount;
    private java.time.LocalDate dueDate;

    /** 行数 (统计) */
    private Integer itemCount;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
