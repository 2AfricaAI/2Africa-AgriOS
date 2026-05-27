-- ============================================================================
-- Migration 029: input_stock - 投入品库存快照表
-- Sprint 22.2a — 按 (input_item × warehouse) 维度跟踪实时库存
-- ============================================================================
-- 设计:
--   每行 = 一个物料在一个仓库(叶子节点)的当前余量
--   qty_on_hand   : 实际在库 (入库加, 出库/损耗减)
--   qty_reserved  : 预留量 (后续 Sprint: PO 收货预留 / Activity 领料预留)
--   available     = qty_on_hand - qty_reserved (前端算, 不存)
--   last_stock_at : 最后一次出入库时间 (用于排序"最近活跃")
--
-- 约束:
--   UNIQUE (input_item_id, warehouse_id) — 同一物料同一仓库只一行
--   软外键: 不加 FK constraint, 靠应用层保证一致性
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `input_stock` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,

  `input_item_id`   BIGINT         NOT NULL
                    COMMENT 'FK -> input_item.id',
  `warehouse_id`    BIGINT         NOT NULL
                    COMMENT 'FK -> location_warehouse.id (leaf stockable node)',

  `qty_on_hand`     DECIMAL(14,3)  NOT NULL DEFAULT 0
                    COMMENT 'Actual quantity in stock (base unit)',
  `qty_reserved`    DECIMAL(14,3)  NOT NULL DEFAULT 0
                    COMMENT 'Reserved quantity (pending outbound, not yet deducted)',

  `last_stock_at`   DATETIME       NULL
                    COMMENT 'Timestamp of last stock movement (in or out)',

  `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME       NULL ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY `uk_item_warehouse` (`input_item_id`, `warehouse_id`),
  KEY `idx_warehouse`       (`warehouse_id`),
  KEY `idx_last_stock`      (`last_stock_at` DESC)
) ENGINE=InnoDB COMMENT='Input item stock level per warehouse — Sprint 22.2';

-- ============================================================================
-- Verify
-- ============================================================================
-- DESC input_stock;
-- SHOW INDEX FROM input_stock;
-- ============================================================================
