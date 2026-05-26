package ai.toafrica.agrios.procurement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商主数据 - Sprint 17.
 *   镜像 Customer (账期 / 联系人 / 状态)
 *   type: input_dealer / labor_contractor / utility / equipment / service / logistics / other
 */
@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** SUP-NNNNN, 自动生成 */
    private String code;

    private String name;

    /** input_dealer / labor_contractor / utility / equipment / service / logistics / other */
    private String type;

    private String taxId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;

    /** 账期天数: 0=COD, 7=周结, 30=月结 */
    private Integer creditDays;
    /** 账期 label (UI 展示用) */
    private String paymentTerms;

    private LocalDate sinceDate;

    /** active / inactive */
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
