package ai.toafrica.agrios.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS / WhatsApp 模板 - Sprint 16.8.
 *   占位符支持: {customerName} {orderCode} {amount} {currency} {dueDate} {daysOverdue}
 */
@Data
@TableName("sms_template")
public class SmsTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** AR_PRE_REMIND / AR_OVERDUE / AR_PROMISE_DUE ... */
    private String code;
    private String name;

    /** sms / whatsapp */
    private String channel;
    /** en / zh / sw */
    private String lang;

    private String content;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
