package ai.toafrica.agrios.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** CUS-NNNNN, 自动生成 */
    private String code;

    private String name;

    /** supermarket / restaurant / ecommerce / wholesale / export / other */
    private String type;

    private String contactName;
    private String contactPhone;

    /** A / B / C / D - 信用等级 */
    private String creditLevel;

    /** 账期天数: 0=COD, 7=周结, 30=月结, 任意自定义 */
    private Integer creditDays;
    /** 账期 label: COD / 周结 / 月结 / Net 30 (UI 展示用) */
    private String paymentTerms;

    /** 最近一次跟催日 (由 CollectionLogService 维护) */
    private LocalDate lastCollectionDate;
    /** 下次跟催日 (由 CollectionLogService 维护, 进 ActionBoard 提醒) */
    private LocalDate nextActionDate;

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
