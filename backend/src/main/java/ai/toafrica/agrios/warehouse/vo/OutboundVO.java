package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OutboundVO {
    private Long id;
    private String code;
    private String sourceType;
    private Long sourceId;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private Long pickedBy;
    private String pickedByName;
    private LocalDateTime pickedAt;
    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private int itemCount;
}
