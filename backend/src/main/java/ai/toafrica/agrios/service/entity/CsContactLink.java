package ai.toafrica.agrios.service.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bridge row mapping an AgriOS business entity (currently always a Customer)
 * to a Chatwoot Contact. See {@code migrations/043_service_module.sql} for the
 * full table doc and rationale.
 */
@Data
@TableName("cs_contact_link")
public class CsContactLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 'customer' in v0.1; reserved for 'supplier' / 'worker' / 'partner' later. */
    private String subjectType;
    private Long subjectId;

    private Long chatwootContactId;
    private Long chatwootAccountId;

    private LocalDateTime lastSyncedAt;
    /** ok / pending / error */
    private String syncStatus;
    private String syncError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
