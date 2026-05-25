package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("planting_plan")
public class PlantingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Long plotId;
    private Long cropId;
    private Long varietyId;
    private BigDecimal areaMu;
    private LocalDate planStartDate;
    private LocalDate planHarvestDate;
    private LocalDate actualStartDate;
    private LocalDate actualFinishDate;
    private BigDecimal targetYieldKg;

    /** draft / planned / in_progress / harvested / completed / cancelled */
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
