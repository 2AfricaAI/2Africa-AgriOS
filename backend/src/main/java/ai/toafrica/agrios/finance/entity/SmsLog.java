package ai.toafrica.agrios.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS / WhatsApp 发送日志 - Sprint 16.8.
 */
@Data
@TableName("sms_log")
public class SmsLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private Long orderId;
    private String templateCode;

    /** sms / whatsapp */
    private String channel;
    private String phone;
    private String content;

    /** africas_talking / twilio / stub */
    private String provider;
    private String providerMsgId;

    /** sent / failed / delivered / unknown */
    private String status;
    private String error;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime sentAt;
    private Long operatorId;
}
