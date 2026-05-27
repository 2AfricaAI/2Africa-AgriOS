package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InboundVO {
    private Long id;
    private String code;
    private String sourceType;
    private Long sourceId;
    private String sourceCode;        // e.g. PO-20260527-0001
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private int itemCount;            // count of detail items
}
