package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecallVO {
    private Long id;
    private String code;
    private LocalDateTime triggeredAt;
    private Long sourceComplaintId;
    private String sourceComplaintCode;

    private Long batchId;
    private String batchCode;
    private String cropName;
    private String varietyName;

    private String scope;
    private String reason;
    private String status;

    private Integer affectedOrderCount;
    private Integer affectedCustomerCount;
    private BigDecimal affectedQty;

    private Long initiatedById;
    private String initiatedByName;
    private LocalDateTime closedAt;
    private Long closedById;
    private String closedByName;
    private String closedRemark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
