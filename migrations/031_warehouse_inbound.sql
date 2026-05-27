-- ============================================================================
-- Migration 031: warehouse_inbound + warehouse_inbound_item
-- Sprint 22.4a — 入库单 (仓库作业: 采购收货 → 验收 → 确认入库)
-- ============================================================================
-- 业务流程:
--   1. PO markReceived → 自动生成 inbound 草稿 (draft)
--   2. 仓库人员验货,填写 actual_qty (可能 ≠ expected_qty)
--   3. 确认入库 (confirmed) → adjustStock(+actual_qty) + 写 stock_log
--   4. 如果验收不合格 → 可取消 (cancelled)
--
-- source_type 枚举:
--   po_receive  — 采购入库 (最常见)
--   return_in   — 客户退货
--   transfer_in — 调拨入库
--   manual      — 手工入库
-- ============================================================================

USE toafrica_agrios;

-- 入库单头表
CREATE TABLE IF NOT EXISTS `warehouse_inbound` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE
                    COMMENT 'IN-YYYYMMDD-NNNN',

  `source_type`     VARCHAR(32)   NOT NULL
                    COMMENT 'po_receive / return_in / transfer_in / manual',
  `source_id`       BIGINT        NULL
                    COMMENT 'Polymorphic: purchase_order.id / transfer.id / NULL',

  `warehouse_id`    BIGINT        NOT NULL
                    COMMENT 'Target warehouse (FK -> location_warehouse.id)',

  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft'
                    COMMENT 'draft / confirmed / cancelled',

  `confirmed_by`    BIGINT        NULL
                    COMMENT 'FK -> sys_user.id (who confirmed)',
  `confirmed_at`    DATETIME      NULL,

  `remark`          VARCHAR(255)  NULL,

  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,

  KEY `idx_inbound_status`  (`status`),
  KEY `idx_inbound_source`  (`source_type`, `source_id`),
  KEY `idx_inbound_wh`      (`warehouse_id`)
) ENGINE=InnoDB COMMENT='Warehouse inbound order — Sprint 22.4';

-- 入库单明细行
CREATE TABLE IF NOT EXISTS `warehouse_inbound_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `inbound_id`      BIGINT         NOT NULL
                    COMMENT 'FK -> warehouse_inbound.id',
  `input_item_id`   BIGINT         NOT NULL
                    COMMENT 'FK -> input_item.id',

  `expected_qty`    DECIMAL(14,3)  NOT NULL
                    COMMENT 'Expected quantity (from PO / source)',
  `actual_qty`      DECIMAL(14,3)  NULL
                    COMMENT 'Actual received qty (filled by warehouse on confirm)',

  `remark`          VARCHAR(255)   NULL,

  KEY `idx_inbound_item`  (`inbound_id`),
  KEY `idx_inbound_ii`    (`input_item_id`)
) ENGINE=InnoDB COMMENT='Warehouse inbound order items — Sprint 22.4';

-- ============================================================================
-- Verify
-- ============================================================================
-- DESC warehouse_inbound;
-- DESC warehouse_inbound_item;
-- ============================================================================
