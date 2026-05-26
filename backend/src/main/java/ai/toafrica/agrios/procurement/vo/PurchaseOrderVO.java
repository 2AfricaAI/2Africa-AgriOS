package ai.toafrica.agrios.procurement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Purchase order list row (header + supplier join)")
public class PurchaseOrderVO {
    private Long id;
    private String code;

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierType;

    private LocalDate orderDate;
    private LocalDate expectedDate;

    private String currency;
    private BigDecimal fxRate;
    private BigDecimal totalAmount;

    private String status;

    private String paymentStatus;
    private BigDecimal paidAmount;
    private LocalDate dueDate;

    private Integer itemCount;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
