-- ============================================================================
-- migration 043: Service module - AgriOS ↔ Chatwoot bridge
--
-- Sprint 40 v0.1 architecture decision:
--   AgriOS does NOT build its own conversation / message / ticket system.
--   Instead, we deploy Chatwoot (MIT-licensed open-source omnichannel CSR
--   platform) as a separate container in our docker-compose, and AgriOS
--   only maintains the THIN bridge described below.
--
-- This migration creates ONLY the two minimum bridge tables:
--
--   1. service_contact_link - maps AgriOS Customer.id ↔ Chatwoot Contact.id
--                              so a Chatwoot conversation can resolve back to
--                              the right AgriOS customer row.
--   2. service_event_log    - audit log of cross-system actions (webhook
--                              received, AgriOS action triggered, sync run,
--                              etc.).  Source of truth for "what happened".
--
-- Everything conversational lives in Chatwoot's own database.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. service_contact_link
--    Two-way join between AgriOS Customer rows and Chatwoot Contact rows.
--    One AgriOS customer typically has one Chatwoot contact, but if the same
--    person reaches us across multiple Chatwoot inboxes/accounts they may end
--    up with several Chatwoot contact ids — keep this as 1:N from our side.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_contact_link (
    id                    BIGINT       PRIMARY KEY AUTO_INCREMENT,
    -- Which AgriOS business entity this Chatwoot contact represents.
    -- Always 'customer' in v0.1, but kept open for future ('supplier',
    -- 'partner', 'worker') so we don't need a migration to extend.
    agrios_entity_type    VARCHAR(32)  NOT NULL DEFAULT 'customer',
    agrios_entity_id      BIGINT       NOT NULL,
    chatwoot_contact_id   BIGINT       NOT NULL,
    chatwoot_account_id   BIGINT       NOT NULL,
    -- When the link was last verified by a successful sync. NULL = never.
    last_synced_at        DATETIME     NULL,
    -- 'ok' / 'pending' / 'error'. Lets the dashboard show broken links.
    sync_status           VARCHAR(16)  NOT NULL DEFAULT 'pending',
    sync_error            VARCHAR(500) NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chatwoot_contact (chatwoot_account_id, chatwoot_contact_id),
    KEY idx_agrios_entity (agrios_entity_type, agrios_entity_id),
    KEY idx_sync_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Service module - AgriOS entity to Chatwoot contact bridge';


-- ----------------------------------------------------------------------------
-- 2. service_event_log
--    Audit trail of every cross-system event:
--      - Inbound webhooks from Chatwoot (new message, conversation resolved)
--      - Outbound actions we triggered (created order, logged complaint)
--      - Sync jobs (customer push, contact pull)
--      - AI decisions (chose to escalate, drafted reply)
--
--    Indexed for two access patterns:
--      a) "Show me everything that happened to this customer"
--      b) "Show me everything in this Chatwoot conversation"
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_event_log (
    id                    BIGINT       PRIMARY KEY AUTO_INCREMENT,
    -- Category of event. Examples:
    --   webhook.message_created
    --   webhook.conversation_resolved
    --   action.create_complaint
    --   action.create_order_draft
    --   sync.customer_push
    --   bot.intent_detected
    --   bot.escalated_to_human
    event_type            VARCHAR(64)  NOT NULL,
    -- direction: inbound (from chatwoot/customer) / outbound (we did)
    direction             VARCHAR(16)  NOT NULL,
    -- Optional references — any combination may be NULL.
    agrios_entity_type    VARCHAR(32)  NULL,
    agrios_entity_id      BIGINT       NULL,
    chatwoot_account_id   BIGINT       NULL,
    chatwoot_conversation_id BIGINT    NULL,
    chatwoot_message_id   BIGINT       NULL,
    -- The full event payload (whatever Chatwoot sent, or our action params).
    payload               JSON         NULL,
    -- ok / failed / skipped — outcome of processing.
    result                VARCHAR(16)  NOT NULL DEFAULT 'ok',
    error_message         VARCHAR(500) NULL,
    -- Idempotency key, used to swallow duplicate webhooks.
    idempotency_key       VARCHAR(128) NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_idempotency (idempotency_key),
    KEY idx_agrios_entity (agrios_entity_type, agrios_entity_id, created_at),
    KEY idx_chatwoot_conv (chatwoot_conversation_id, created_at),
    KEY idx_event_type (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Service module - cross-system event audit log';
