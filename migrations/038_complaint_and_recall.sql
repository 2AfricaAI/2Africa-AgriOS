-- Sprint 27 — Complaints & Recall
-- complaint: customer-facing or internal-QC issue tied to an order / batch / sku.
-- recall:    quarantine + downstream notification triggered by complaint or QC.
--
-- NOTE: target database is selected via the mysql CLI / connection — no USE statement here.

-- ------------------------------------------------------------
-- complaint
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `complaint` (
  `id`                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `code`                VARCHAR(32)    NOT NULL UNIQUE COMMENT 'COMPL-YYYYMMDD-NNNN',
  `reported_at`         DATETIME       NOT NULL,
  `customer_id`         BIGINT         NULL  COMMENT 'NULL = internal QC complaint',
  `order_id`            BIGINT         NULL,
  `batch_id`            BIGINT         NULL,
  `sku_id`              BIGINT         NULL,
  `category`            VARCHAR(32)    NOT NULL COMMENT 'quality / quantity / late / safety / wrong_product / other',
  `severity`            VARCHAR(16)    NOT NULL DEFAULT 'medium' COMMENT 'low / medium / high / critical',
  `channel`             VARCHAR(16)    NOT NULL DEFAULT 'phone' COMMENT 'phone / email / app / onsite / other',
  `description`         TEXT           NOT NULL,
  `photo_ids`           JSON           NULL COMMENT 'File IDs for evidence photos',
  `status`              VARCHAR(24)    NOT NULL DEFAULT 'open'
                                       COMMENT 'open / investigating / resolved / closed / escalated_to_recall',
  `resolution`          TEXT           NULL,
  `resolution_amount`   DECIMAL(12,2)  NULL COMMENT 'Refund or credit amount, KES',
  `reported_by_id`      BIGINT         NULL,
  `resolved_at`         DATETIME       NULL,
  `resolved_by_id`      BIGINT         NULL,
  `recall_id`           BIGINT         NULL COMMENT 'Set when escalated to a recall',
  `created_at`          DATETIME       DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_customer`    (`customer_id`),
  KEY `idx_order`       (`order_id`),
  KEY `idx_batch`       (`batch_id`),
  KEY `idx_status`      (`status`),
  KEY `idx_reported_at` (`reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer / QC complaints';

-- ------------------------------------------------------------
-- recall — batch-level quarantine + downstream notification
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `recall` (
  `id`                   BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                 VARCHAR(32)  NOT NULL UNIQUE COMMENT 'RECALL-YYYYMMDD-NNNN',
  `triggered_at`         DATETIME     NOT NULL,
  `source_complaint_id`  BIGINT       NULL COMMENT 'NULL = manually initiated by QC',
  `batch_id`             BIGINT       NOT NULL,
  `scope`                VARCHAR(24)  NOT NULL DEFAULT 'batch_only'
                                      COMMENT 'batch_only / batch_plus_children',
  `reason`               TEXT         NOT NULL,
  `status`               VARCHAR(24)  NOT NULL DEFAULT 'initiated'
                                      COMMENT 'initiated / quarantined / customers_notified / closed',
  `affected_order_count` INT          NOT NULL DEFAULT 0,
  `affected_customer_count` INT       NOT NULL DEFAULT 0,
  `affected_qty`         DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT 'Total frozen quantity',
  `initiated_by_id`      BIGINT       NULL,
  `closed_at`            DATETIME     NULL,
  `closed_by_id`         BIGINT       NULL,
  `closed_remark`        VARCHAR(500) NULL,
  `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_batch`        (`batch_id`),
  KEY `idx_status`       (`status`),
  KEY `idx_triggered_at` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Batch recall — quarantine + downstream notification';

-- ------------------------------------------------------------
-- recall_affected_order — snapshot of downstream orders at time of recall
-- (so the report stays consistent even if orders later change state)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `recall_affected_order` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `recall_id`       BIGINT        NOT NULL,
  `order_id`        BIGINT        NOT NULL,
  `order_code`      VARCHAR(64)   NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `customer_name`   VARCHAR(128)  NOT NULL,
  `qty`             DECIMAL(14,3) NOT NULL,
  `unit`            VARCHAR(8)    DEFAULT 'pack',
  `delivered_at`    DATETIME      NULL COMMENT 'From fulfillment.shipped_at if available',
  `notified_at`     DATETIME      NULL COMMENT 'Set when sales confirms customer was notified',
  `notified_by_id`  BIGINT        NULL,
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_recall`   (`recall_id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recall affected orders snapshot';
