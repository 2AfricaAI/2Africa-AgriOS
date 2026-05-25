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
@TableName("batch")
public class Batch {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** B-yyyyMMdd-{plotCode}-NN */
    private String code;

    /** 拆分场景 - 由其他 batch 拆出来时的来源 */
    private Long parentBatchId;

    private Long plotId;
    private Long planId;
    private Long cropId;
    private Long varietyId;
    private Long harvestRecordId;

    private LocalDate harvestDate;
    private BigDecimal qtyKg;
    private BigDecimal qtyRemainKg;

    /** pending / processing / packed / sold_out / lost */
    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}
