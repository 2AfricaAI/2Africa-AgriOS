package ai.toafrica.agrios.sales.entity;

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
@TableName("sales_order")
public class SalesOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** SO-yyyyMMdd-NNN */
    private String code;

    private Long customerId;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String shipTo;

    /** KES / USD / EUR */
    private String currency;

    /** Σ(item.amount) */
    private BigDecimal totalAmount;

    /** pending / confirmed / locked / shipping / shipped / delivered / completed / cancelled / returned */
    private String status;

    /** unpaid / partial / paid - 由 PaymentService 自动维护 */
    private String paymentStatus;
    /** 累计已收款 (KES 本位币) - 由 PaymentService 自动累加 */
    private BigDecimal paidAmount;
    /** 应付日 = order_date + customer.credit_days, 在订单创建时计算 */
    private java.time.LocalDate dueDate;

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
