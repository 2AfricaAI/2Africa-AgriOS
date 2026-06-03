package ai.toafrica.agrios.service.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 50d -- a single CSAT (Customer Satisfaction) record. One row
 * is created at link-generation time with rating=null; the customer
 * fills in rating + optional comment via the public survey page.
 *
 * <p>Schema: see {@code migrations/049_cs_csat.sql}.</p>
 */
@Data
@TableName("cs_csat_response")
public class CsCsatResponse {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 24-char URL-safe base32 opaque token. Unique. */
    private String token;

    private Long chatwootConversationId;
    private Long chatwootContactId;
    private Long agriosCustomerId;
    /** Agent at link-gen time -- attribution anchor that survives reassignment. */
    private Long chatwootAgentId;

    /** 1..5, null until the customer submits. */
    private Integer rating;
    /** Optional free-form comment, capped at 2000 chars server-side. */
    private String comment;

    /** Filled at submit time. Null = link issued, not yet acted on. */
    private LocalDateTime submittedAt;
    /** TTL gate (link-gen + 30 days). */
    private LocalDateTime expiresAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
