package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComplaintVO {
    private Long id;
    private String code;
    private LocalDateTime reportedAt;

    private Long customerId;
    private String customerName;

    private Long orderId;
    private String orderCode;

    private Long batchId;
    private String batchCode;

    private Long skuId;
    private String skuCode;
    private String skuName;

    private String category;
    private String severity;
    private String channel;
    private String description;
    private List<Long> photoIds;

    private String status;
    private String resolution;
    private BigDecimal resolutionAmount;

    private Long reportedById;
    private String reportedByName;
    private LocalDateTime resolvedAt;
    private Long resolvedById;
    private String resolvedByName;

    private Long recallId;
    private String recallCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
