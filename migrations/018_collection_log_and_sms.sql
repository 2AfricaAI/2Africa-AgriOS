-- ============================================================================
-- Sprint 16 - AR 催收闭环
--   1. collection_log: 催收跟催记录 (电话/WhatsApp/SMS/邮件/上门)
--   2. customer:       加 last_collection_date / next_action_date 两个冗余冷字段
--   3. sms_template:   SMS / WhatsApp 模板 (催收预提醒 / 逾期 N 天 / 承诺到期)
--   4. sms_log:        发送日志 (轻量, 仅记录, 不强依赖真实 Provider)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 跟催记录
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `collection_log` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `customer_id`      BIGINT        NOT NULL                COMMENT 'Customer id',
  `order_id`         BIGINT                                COMMENT 'Order id (optional; leave empty for general follow-up)',
  `log_date`         DATE          NOT NULL                COMMENT 'Follow-up date',
  `channel`          VARCHAR(16)   NOT NULL
                     COMMENT 'phone / whatsapp / sms / email / visit / other',
  `contact_person`   VARCHAR(80)                           COMMENT 'Actual contact person reached',
  `outcome`          VARCHAR(32)   NOT NULL
                     COMMENT 'promised / refused / no_answer / disputed / paid / other',
  `promised_date`    DATE                                  COMMENT 'Customer-promised payment date (feeds cash-flow forecast)',
  `promised_amount`  DECIMAL(14,2)                         COMMENT 'Promised payment amount',
  `content`          TEXT                                  COMMENT 'Conversation content / remark',
  `next_action_date` DATE                                  COMMENT 'Next follow-up date',
  `operator_id`      BIGINT        NOT NULL                COMMENT 'Follow-up agent (user_id)',
  `operator_name`    VARCHAR(80)                           COMMENT 'Denormalized follow-up agent name for fast display',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME                              ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`       DATETIME                              COMMENT 'Soft delete',
  KEY `idx_customer`        (`customer_id`, `log_date`),
  KEY `idx_order`           (`order_id`),
  KEY `idx_next_action`     (`next_action_date`),
  KEY `idx_promised_date`   (`promised_date`)
) ENGINE=InnoDB COMMENT='Collection follow-up log - Sprint 16';

-- ----------------------------------------------------------------------------
-- 2. customer 加 2 个冷字段 (避免每次查跟催历史; 由 CollectionLogService 维护)
-- ----------------------------------------------------------------------------
ALTER TABLE `customer`
  ADD COLUMN `last_collection_date` DATE
             COMMENT 'Last follow-up date'      AFTER `payment_terms`,
  ADD COLUMN `next_action_date`     DATE
             COMMENT 'Next follow-up date'          AFTER `last_collection_date`;

-- ----------------------------------------------------------------------------
-- 3. SMS / WhatsApp 模板
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sms_template` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`         VARCHAR(32)  NOT NULL UNIQUE
                 COMMENT 'AR_PRE_REMIND / AR_OVERDUE / AR_PROMISE_DUE ...',
  `name`         VARCHAR(80)  NOT NULL                COMMENT 'Display name',
  `channel`      VARCHAR(16)  NOT NULL DEFAULT 'sms'
                 COMMENT 'sms / whatsapp',
  `lang`         VARCHAR(8)   NOT NULL DEFAULT 'en'   COMMENT 'en / zh / sw',
  `content`      VARCHAR(500) NOT NULL
                 COMMENT 'Template body — supports {customerName} {orderCode} {amount} {dueDate} {daysOverdue} placeholders',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME                              ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='SMS / WhatsApp templates';

-- ----------------------------------------------------------------------------
-- 4. SMS 发送日志 (轻量, 不强依赖 Provider 真实回执)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sms_log` (
  `id`             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `customer_id`    BIGINT       NOT NULL,
  `order_id`       BIGINT                              COMMENT 'Optional order linkage',
  `template_code`  VARCHAR(32)                         COMMENT 'Which template was used',
  `channel`        VARCHAR(16)  NOT NULL DEFAULT 'sms',
  `phone`          VARCHAR(32)  NOT NULL               COMMENT '+254...',
  `content`        VARCHAR(500) NOT NULL               COMMENT 'Final sent text (placeholders resolved)',
  `provider`       VARCHAR(32)                         COMMENT 'africas_talking / twilio / stub',
  `provider_msg_id` VARCHAR(64)                        COMMENT 'Provider-returned message id',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'sent'
                   COMMENT 'sent / failed / delivered / unknown',
  `error`          VARCHAR(255),
  `sent_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `operator_id`    BIGINT,
  KEY `idx_customer` (`customer_id`, `sent_at`),
  KEY `idx_status`   (`status`)
) ENGINE=InnoDB COMMENT='SMS / WhatsApp send log';

-- ----------------------------------------------------------------------------
-- 5. 预置 3 个英文模板 (Kenya 主市场)
--    占位符: {customerName} {orderCode} {amount} {currency} {dueDate} {daysOverdue}
-- ----------------------------------------------------------------------------
INSERT INTO `sms_template` (`code`, `name`, `channel`, `lang`, `content`) VALUES
  ('AR_PRE_REMIND',
   'Pre-due reminder (3 days before)',
   'sms', 'en',
   'Hi {customerName}, this is a friendly reminder that invoice {orderCode} ({currency} {amount}) is due on {dueDate}. Thank you for your continued business. - 2Africa AgriOS'),
  ('AR_OVERDUE',
   'Overdue reminder',
   'sms', 'en',
   'Dear {customerName}, invoice {orderCode} ({currency} {amount}) was due on {dueDate} and is now {daysOverdue} days overdue. Please arrange payment. - 2Africa AgriOS'),
  ('AR_PROMISE_DUE',
   'Promise-to-pay follow-up',
   'sms', 'en',
   'Hi {customerName}, just following up on your promise to settle invoice {orderCode} ({currency} {amount}). Kindly confirm payment status today. - 2Africa AgriOS');
