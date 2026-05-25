package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "plot", autoResultMap = true)
public class Plot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private BigDecimal areaMu;
    private String location;
    private String soilType;
    private String irrigation;
    private Long ownerId;

    /** JSON 字段：可种植作物 ID 数组 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.List<Long> allowedCrops;

    /** active / inactive / fallow */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}
