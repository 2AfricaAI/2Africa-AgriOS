package ai.toafrica.agrios.finance.entity;

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
 * 回款流水 - V2.0 Phase 2 应收账款核心.
 *   一个 order 可有 N 次 payment (定金 + 尾款 + 退款 ...)
 *   AR 余额 = order.total_amount - SUM(payment.amount_kes for cleared payments)
 */
@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Long orderId;
    private Long customerId;

    private BigDecimal amount;
    private String currency;
    private BigDecimal fxRate;
    private BigDecimal amountKes;

    /** cash / bank / cheque / loop_online / loop_pos */
    private String method;
    private LocalDate paymentDate;
    /** Loop 回执号 / 银行流水 / 支票号 */
    private String referenceNo;
    /** POS 机标识 (loop_pos 才用) */
    private String posTerminalId;
    /** Loop 聚合后的实际通道: mpesa / card / bank (webhook 写入) */
    private String channel;

    /** pending / partial / cleared / bad_debt / reversed */
    private String status;

    private Long reconciledBy;
    private LocalDateTime reconciledAt;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
