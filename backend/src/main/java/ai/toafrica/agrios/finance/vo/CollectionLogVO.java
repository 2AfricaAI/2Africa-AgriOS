package ai.toafrica.agrios.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Collection log row (with customer / order joins)")
public class CollectionLogVO {
    private Long id;

    private Long customerId;
    private String customerCode;
    private String customerName;

    private Long orderId;
    private String orderCode;

    private LocalDate logDate;
    private String channel;
    private String contactPerson;
    private String outcome;

    private LocalDate promisedDate;
    private BigDecimal promisedAmount;

    private String content;
    private LocalDate nextActionDate;

    private Long operatorId;
    private String operatorName;

    private LocalDateTime createdAt;
}
