package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农事-投入品明细 (Sprint 23f).
 *
 * 一条 Activity (尤其是 spray / fertilize) 可以挂多个 input_item,
 * 每条记录用了多少 qty + 单位 + 成本.
 *
 * PHI 检查依赖此表: 喷药活动必须有这里的明细行才能算 PHI.
 */
@Data
@TableName("activity_input")
public class ActivityInput {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;
    /** FK -> input_item.id (列名是 input_id, 兼容老 schema) */
    @TableField("input_id")
    private Long inputItemId;

    private BigDecimal qty;
    private String unit;
    private BigDecimal cost;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
