package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投入品主数据 (Sprint 21.1, Phase 4)
 *
 * Replaces the old enum-only input_type with a full SKU master:
 *   - spec / unit / active ingredient
 *   - PHI (Pre-Harvest Interval) for pesticides
 *   - registration number (Kenya PCPB compliance)
 *   - default supplier link
 */
@Data
@TableName("input_item")
public class InputItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** II-0001 */
    private String code;

    /** 本地名 e.g. "尿素 46-0-0" */
    private String name;

    /** 英文名 e.g. "Urea 46-0-0" */
    private String nameEn;

    /** fertilizer/pesticide/seed/film/labor/other */
    private String inputType;

    /** 规格 e.g. "50kg/bag" */
    private String spec;

    /** kg/L/pack/box/pcs */
    private String unit;

    /** 有效成分 (主要给农药用) */
    private String activeIngredient;

    /** 农药登记证号 (Kenya PCPB 等) */
    private String registrationNo;

    /** Pre-Harvest Interval - 喷药安全期天数, 仅 pesticide 有意义 */
    private Integer phiDays;

    /** 默认供应商 FK -> supplier.id (可选) */
    private Long defaultSupplierId;

    /** active / inactive */
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
}
