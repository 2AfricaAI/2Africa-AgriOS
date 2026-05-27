-- ============================================================================
-- Migration 032: warehouse_outbound + warehouse_outbound_item
-- Sprint 22.5 — 出库单 (Outbound Management)
-- ============================================================================
-- 业务流程:
--   1. Activity 领料 / Sales 出货 → 自动生成 outbound 草稿 (draft)
--   2. 仓库人员拣货 (picked) → 填 picked_qty
--   3. 确认出库 (confirmed) → adjustStock(-actual_qty) + 写 stock_log
--   4. 取消 (cancelled)
--
-- source_type 枚举:
--   activity_consume — 农事领料出库
--   sales_ship       — 销售发货出库
--   transfer_out     — 调拨出库
--   manual           — 手工出库
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `warehouse_outbound` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE
                    COMMENT 'OUT-YYYYMMDD-NNNN',

  `source_type`     VARCHAR(32)   NOT NULL
                    COMMENT 'activity_consume / sales_ship / transfer_out / manual',
  `source_id`       BIGINT        NULL
                    COMMENT 'Polymorphic: activity.id / sales_order.id / transfer.id / NULL',

  `warehouse_id`    BIGINT        NOT NULL
                    COMMENT 'Source warehouse (FK -> location_warehouse.id)',

  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft'
                    COMMENT 'draft / picked / confirmed / cancelled',

  `picked_by`       BIGINT        NULL COMMENT 'FK -> sys_user.id (picker)',
  `picked_at`       DATETIME      NULL,
  `confirmed_by`    BIGINT        NULL COMMENT 'FK -> sys_user.id (confirmer)',
  `confirmed_at`    DATETIME      NULL,

  `remark`          VARCHAR(255)  NULL,

  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,

  KEY `idx_outbound_status`  (`status`),
  KEY `idx_outbound_source`  (`source_type`, `source_id`),
  KEY `idx_outbound_wh`      (`warehouse_id`)
) ENGINE=InnoDB COMMENT='Warehouse outbound order — Sprint 22.5';

CREATE TABLE IF NOT EXISTS `warehouse_outbound_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `outbound_id`     BIGINT         NOT NULL
                    COMMENT 'FK -> warehouse_outbound.id',
  `input_item_id`   BIGINT         NOT NULL
                    COMMENT 'FK -> input_item.id',

  `requested_qty`   DECIMAL(14,3)  NOT NULL
                    COMMENT 'Requested quantity (from activity / sales)',
  `picked_qty`      DECIMAL(14,3)  NULL
                    COMMENT 'Picked quantity (filled by picker)',
  `actual_qty`      DECIMAL(14,3)  NULL
                    COMMENT 'Actual issued qty (filled on confirm, usually = picked)',

  `remark`          VARCHAR(255)   NULL,

  KEY `idx_outbound_item`  (`outbound_id`),
  KEY `idx_outbound_ii`    (`input_item_id`)
) ENGINE=InnoDB COMMENT='Warehouse outbound order items — Sprint 22.5';
