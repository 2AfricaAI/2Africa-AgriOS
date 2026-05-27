-- ============================================================================
-- Migration 034: warehouse_transfer + warehouse_transfer_item
-- Sprint 22.7 — Transfer Management (调拨管理)
-- ============================================================================
-- 流程: draft → confirmed (同时: 源仓 -qty + 目标仓 +qty + 2条 stock_log)
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `warehouse_transfer` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE COMMENT 'TR-YYYYMMDD-NNNN',
  `from_warehouse_id` BIGINT     NOT NULL COMMENT 'Source warehouse',
  `to_warehouse_id`   BIGINT     NOT NULL COMMENT 'Target warehouse',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / cancelled',
  `confirmed_by`    BIGINT        NULL,
  `confirmed_at`    DATETIME      NULL,
  `remark`          VARCHAR(255)  NULL,
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_tr_status` (`status`),
  KEY `idx_tr_from`   (`from_warehouse_id`),
  KEY `idx_tr_to`     (`to_warehouse_id`)
) ENGINE=InnoDB COMMENT='Warehouse transfer order — Sprint 22.7';

CREATE TABLE IF NOT EXISTS `warehouse_transfer_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `transfer_id`     BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `qty`             DECIMAL(14,3)  NOT NULL COMMENT 'Transfer quantity',
  `remark`          VARCHAR(255)   NULL,
  KEY `idx_tri_parent` (`transfer_id`),
  KEY `idx_tri_item`   (`input_item_id`)
) ENGINE=InnoDB COMMENT='Transfer order items — Sprint 22.7';
