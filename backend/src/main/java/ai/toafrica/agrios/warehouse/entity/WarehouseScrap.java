package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("warehouse_scrap")
public class WarehouseScrap {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long warehouseId;
    private String scrapType;
    private String status;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
