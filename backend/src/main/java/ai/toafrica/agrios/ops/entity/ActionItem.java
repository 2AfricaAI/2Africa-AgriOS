package ai.toafrica.agrios.ops.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经营行动清单条目 - V2.0 模块十一 / Sprint 10.
 *
 * 由规则引擎周期性写入 (rule_code + ref_type + ref_id 三元组唯一).
 * 每次刷新时:
 *   - 命中规则 → upsert (status=open 持续亮灯)
 *   - 之前 open 但本次未命中 → auto_resolved
 *
 * 用户在前端可对单条 action 标记为 done 或 dismissed.
 */
@Data
@TableName("action_item")
public class ActionItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** R-INV-01 / R-PROD-02 / R-CUST-01 ... 规则编码 */
    private String ruleCode;

    /** high / medium / low */
    private String severity;

    /** today / week_risk / followup / pause - 决定显示在哪个 tab */
    private String category;

    private String title;
    private String description;

    /** sales / packhouse / finance / ceo / production / qc */
    private String ownerRole;

    /** inventory / sku / customer / batch / order / ... */
    private String refType;
    private Long   refId;
    /** 业务码, 方便前端展示和跳转 (如 batch.code / customer.code) */
    private String refCode;

    /** open / done / dismissed / auto_resolved */
    private String status;

    private LocalDate dueDate;

    /** JSON 文本: 该 action 触发时的快照, 帮助 audit 与未来 ML */
    private String dataSnapshot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;
    private Long          resolvedBy;
    private String        resolvedRemark;
}
