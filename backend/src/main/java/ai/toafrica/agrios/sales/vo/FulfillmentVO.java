package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Fulfillment list row (with order + customer join)")
public class FulfillmentVO {
    private Long id;
    private String code;

    private Long orderId;
    private String orderCode;

    private Long customerId;
    private String customerName;
    private String customerCode;

    private Long pickerId;
    private String pickerName;

    private LocalDateTime planShipAt;
    private LocalDateTime shipAt;
    private LocalDateTime deliveredAt;

    private String status;
    private String shipMethod;
    private String trackNo;
    private String driverName;
    private String driverPhone;
    private String vehicleNo;
    private String remark;

    /** 行数 */
    private Integer itemCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
