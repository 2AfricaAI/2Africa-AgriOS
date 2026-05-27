-- ============================================================================
-- Migration 033: warehouse_stocktake + warehouse_stocktake_item
-- Sprint 22.6 — Inventory Count (盘点管理)
-- ============================================================================
-- 流程:
--   1. 创建盘点单 (draft) → 系统自动拍快照 system_qty
--   2. 盘点人员实地盘点 → 填 count_qty (counting)
--   3. 确认 (confirmed) → diff = count - system → adjustStock(diff) + log
--
-- count_type: full (全盘) / cycle (循环盘点) / random (抽盘)
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `warehouse_stocktake` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE
                    COMMENT 'ST-YYYYMMDD-NNNN',

  `warehouse_id`    BIGINT        NOT NULL
                    COMMENT 'FK -> location_warehouse.id',
  `count_type`      VARCHAR(16)   NOT NULL DEFAULT 'full'
                    COMMENT 'full / cycle / random',

  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft'
                    COMMENT 'draft / counting / confirmed / cancelled',

  `counted_by`      BIGINT        NULL,
  `counted_at`      DATETIME      NULL,
  `confirmed_by`    BIGINT        NULL,
  `confirmed_at`    DATETIME      NULL,

  `remark`          VARCHAR(255)  NULL,
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,

  KEY `idx_st_status`    (`status`),
  KEY `idx_st_warehouse` (`warehouse_id`)
) ENGINE=InnoDB COMMENT='Warehouse stocktake / inventory count — Sprint 22.6';

CREATE TABLE IF NOT EXISTS `warehouse_stocktake_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `stocktake_id`    BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,

  `system_qty`      DECIMAL(14,3)  NOT NULL
                    COMMENT 'System qty_on_hand at snapshot time',
  `count_qty`       DECIMAL(14,3)  NULL
                    COMMENT 'Physical count qty (filled by counter)',
  `diff_qty`        DECIMAL(14,3)  NULL
                    COMMENT '= count_qty - system_qty (computed on confirm)',

  `remark`          VARCHAR(255)   NULL,

  KEY `idx_sti_parent`  (`stocktake_id`),
  KEY `idx_sti_item`    (`input_item_id`)
) ENGINE=InnoDB COMMENT='Stocktake line items — Sprint 22.6';
