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
 * 催收跟催记录 - Sprint 16
 *   - 一个客户可有 N 次跟催 (电话 / WhatsApp / SMS / 邮件 / 上门)
 *   - 可关联具体订单, 也可综合跟催 (orderId 留空)
 *   - 承诺还款日 + 承诺金额 → 进 13 周现金流预测
 */
@Data
@TableName("collection_log")
public class CollectionLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private Long orderId;

    private LocalDate logDate;

    /** phone / whatsapp / sms / email / visit / other */
    private String channel;
    private String contactPerson;

    /** promised / refused / no_answer / disputed / paid / other */
    private String outcome;

    private LocalDate promisedDate;
    private BigDecimal promisedAmount;

    private String content;
    private LocalDate nextActionDate;

    private Long operatorId;
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
