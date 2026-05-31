package ai.toafrica.agrios.service.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cross-system event audit row. See {@code migrations/043_service_module.sql}
 * for the full doc.
 */
@Data
@TableName("service_event_log")
public class ServiceEventLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Examples: sync.customer_push, webhook.message_created, action.create_complaint */
    private String eventType;

    /** inbound / outbound */
    private String direction;

    private String agriosEntityType;
    private Long agriosEntityId;

    private Long chatwootAccountId;
    private Long chatwootConversationId;
    private Long chatwootMessageId;

    /** JSON payload string. We store it as text and let Jackson handle marshalling at the edges. */
    private String payload;

    /** ok / failed / skipped */
    private String result;
    private String errorMessage;

    /** Idempotency key to swallow duplicate webhooks. Unique index in DB. */
    private String idempotencyKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
