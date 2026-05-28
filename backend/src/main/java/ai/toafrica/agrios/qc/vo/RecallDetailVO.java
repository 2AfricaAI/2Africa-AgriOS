package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecallDetailVO {
    private RecallVO recall;
    private List<AffectedOrder> affectedOrders;

    @Data
    public static class AffectedOrder {
        private Long id;
        private Long orderId;
        private String orderCode;
        private Long customerId;
        private String customerName;
        private BigDecimal qty;
        private String unit;
        private LocalDateTime deliveredAt;
        private LocalDateTime notifiedAt;
        private Long notifiedById;
        private String notifiedByName;
    }
}
