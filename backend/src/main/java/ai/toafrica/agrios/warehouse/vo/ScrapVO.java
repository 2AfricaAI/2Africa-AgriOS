package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScrapVO {
    private Long id;
    private String code;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String scrapType;
    private String status;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private int itemCount;
}
