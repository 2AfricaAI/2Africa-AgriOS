package ai.toafrica.agrios.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse_inbound")
public class WarehouseInbound {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String sourceType;
    private Long sourceId;
    private Long warehouseId;
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
