package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
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

    /** Sprint 22.1.5: L2 子分类 (fertilizer→nitrogen/phosphate/compound/organic; pesticide→herbicide/insecticide/fungicide) */
    private String categoryL2;

    /** 规格 e.g. "50kg/bag" (自由文本) */
    private String spec;

    /** Sprint 22.1.5: 单包数量 (如 50 表示 50kg/袋) */
    private BigDecimal packQty;

    /** Sprint 22.1.5: 包装单位 (bag/bottle/box/can/sack) */
    private String packUnitLabel;

    /** kg/L/pack/box/pcs (基本单位) */
    private String unit;

    /** 有效成分 (主要给农药用) */
    private String activeIngredient;

    /** 农药登记证号 (Kenya PCPB 等) */
    private String registrationNo;

    /** Pre-Harvest Interval - 喷药安全期天数, 仅 pesticide 有意义 */
    private Integer phiDays;

    /** 默认供应商 FK -> supplier.id (可选) */
    private Long defaultSupplierId;

    /** Sprint 22.1.5: 默认入库仓 FK -> location_warehouse.id */
    private Long defaultWarehouseId;

    /** Sprint 22.1.5: 库存预警阈值 (base unit, R-INV-04 用) */
    private BigDecimal minStockQty;

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
