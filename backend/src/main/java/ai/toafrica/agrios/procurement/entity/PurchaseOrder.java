package ai.toafrica.agrios.procurement.entity;

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

/**
 * 采购订单 - Sprint 17.3.
 *   状态机:
 *     draft → confirmed → received / partial_received → (closed)
 *                                 ↘ cancelled
 *   payment_status: 由 VendorPaymentService 自动维护
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** PO-YYYYMMDD-NNNN */
    private String code;

    private Long supplierId;
    private LocalDate orderDate;
    private LocalDate expectedDate;

    /** KES / USD / EUR */
    private String currency;
    private BigDecimal fxRate;

    /** Σ(item.amount) - 订单币种 */
    private BigDecimal totalAmount;

    /** draft / confirmed / partial_received / received / cancelled */
    private String status;

    /** unpaid / partial / paid - 由 VendorPaymentService 维护 */
    private String paymentStatus;
    /** 累计已付 (KES 本位币) */
    private BigDecimal paidAmount;
    /** 应付日 = order_date + supplier.credit_days */
    private LocalDate dueDate;

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
