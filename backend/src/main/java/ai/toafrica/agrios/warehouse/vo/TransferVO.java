package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransferVO {
    private Long id;
    private String code;
    private Long fromWarehouseId;
    private String fromWarehouseCode;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseCode;
    private String toWarehouseName;
    private String status;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private int itemCount;
}
