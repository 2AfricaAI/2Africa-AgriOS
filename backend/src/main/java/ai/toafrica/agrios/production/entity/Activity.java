package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "activity", autoResultMap = true)
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键 (前端生成 UUID,后端去重) */
    private String clientUuid;

    private Long plotId;
    private Long planId;

    /** sow / fertilize / spray / weed / water / prune / other */
    private String activityType;

    private LocalDate occurDate;

    /** 实际操作人 (当前阶段用 sys_user.id; 等 staff 表上线后切换) */
    private Long operatorId;

    /** 照片 - JSON 列存 file IDs 数组,例如 [1,2,3] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> photos;

    private String locationGps;
    private String remark;

    /** 人工成本 (V2.0 Phase 2 P&L) */
    private BigDecimal laborCost;
    /** 关联 PO 行 (Sprint 17.7) */
    private Long laborPoItemId;
    /** 水费 */
    private BigDecimal waterCost;
    private Long waterPoItemId;
    /** 电费 */
    private BigDecimal electricityCost;
    private Long electricityPoItemId;
    /** 肥料成本 */
    private BigDecimal fertilizerCost;
    private Long fertilizerPoItemId;
    /** 其他成本 */
    private BigDecimal otherCost;
    private Long otherPoItemId;
    /** 所有成本字段共用的货币 (与 activity_input.currency 独立) */
    private String costCurrency;

    /** pending / approved / rejected */
    private String auditStatus;
    private Long auditorId;
    private LocalDateTime auditedAt;
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
