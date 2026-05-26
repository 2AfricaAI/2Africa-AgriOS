package ai.toafrica.agrios.procurement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付供应商款 - Sprint 17.5 (镜像 Payment).
 *   一个 PO 可有 N 次 vendor_payment (定金 + 尾款 ...).
 *   AP 余额 = po.total_amount - SUM(vendor_payment.amount_kes for cleared payments).
 */
@Data
@TableName("vendor_payment")
public class VendorPayment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Long poId;
    private Long supplierId;

    private BigDecimal amount;
    private String currency;
    private BigDecimal fxRate;
    private BigDecimal amountKes;

    /** cash / bank / cheque / loop_online / loop_pos */
    private String method;
    private LocalDate paymentDate;
    /** 银行流水号 / Loop 回执 / 支票号 */
    private String referenceNo;
    /** POS 终端号 (loop_pos 才用) */
    private String posTerminalId;
    /** mpesa / card / bank (Loop 聚合后实际通道) */
    private String channel;

    /** pending / cleared / reversed */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
