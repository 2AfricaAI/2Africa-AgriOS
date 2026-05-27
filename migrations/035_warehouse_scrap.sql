-- ============================================================================
-- Migration 035: warehouse_scrap + warehouse_scrap_item
-- Sprint 22.8 — Scrap Management (报废管理)
-- ============================================================================
-- scrap_type: damaged (残损) / expired (过期) / other
-- 流程: draft → approved → confirmed (扣库存) / rejected
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `warehouse_scrap` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE COMMENT 'SC-YYYYMMDD-NNNN',
  `warehouse_id`    BIGINT        NOT NULL,
  `scrap_type`      VARCHAR(16)   NOT NULL DEFAULT 'damaged' COMMENT 'damaged / expired / other',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / cancelled',
  `confirmed_by`    BIGINT        NULL,
  `confirmed_at`    DATETIME      NULL,
  `remark`          VARCHAR(255)  NULL,
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_sc_status`    (`status`),
  KEY `idx_sc_warehouse` (`warehouse_id`)
) ENGINE=InnoDB COMMENT='Warehouse scrap / write-off order — Sprint 22.8';

CREATE TABLE IF NOT EXISTS `warehouse_scrap_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `scrap_id`        BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `qty`             DECIMAL(14,3)  NOT NULL COMMENT 'Scrap quantity',
  `reason`          VARCHAR(255)   NULL COMMENT 'Reason for scrap (per item)',
  KEY `idx_sci_parent` (`scrap_id`),
  KEY `idx_sci_item`   (`input_item_id`)
) ENGINE=InnoDB COMMENT='Scrap order items — Sprint 22.8';
