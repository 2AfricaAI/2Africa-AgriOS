-- ============================================================================
-- Migration 028: input_item 补 5 个字段
-- Sprint 22.1.5 — 为 Sprint 22.2 (input_stock 库存表) 铺路
-- ============================================================================
-- 加的字段:
--   1) category_l2          - L2 子分类 (fertilizer 细分: nitrogen/phosphate/compound/organic)
--   2) default_warehouse_id - 默认入库仓 FK (22.4 PO 收货自动入库用)
--   3) min_stock_qty        - 库存预警阈值 (R-INV-04 低库存规则用)
--   4) pack_qty             - 单包数量 (如 50 表示 50kg/袋)
--   5) pack_unit_label      - 包装单位 (bag/bottle/box/...)
--
-- 设计原则:
--   - 全部 NULL 允许 (老数据不破坏)
--   - default_warehouse_id 软外键,不加 FK 约束
--   - pack_qty + pack_unit_label 一起描述包装,但保留原 spec 自由文本字段
-- ============================================================================

USE toafrica_agrios;

ALTER TABLE `input_item`
  ADD COLUMN `category_l2`          VARCHAR(64)    NULL
    COMMENT 'L2 sub-category (fertilizer:nitrogen/phosphate/compound/organic; pesticide:herbicide/insecticide/fungicide; seed:vegetable/fruit/flower)'
    AFTER `input_type`,
  ADD COLUMN `pack_qty`             DECIMAL(14,3)  NULL
    COMMENT 'Quantity per pack (e.g., 50 for 50kg/bag)'
    AFTER `spec`,
  ADD COLUMN `pack_unit_label`      VARCHAR(32)    NULL
    COMMENT 'Pack label (bag/bottle/box/can/sack/...)'
    AFTER `pack_qty`,
  ADD COLUMN `default_warehouse_id` BIGINT         NULL
    COMMENT 'FK -> location_warehouse.id (default storage location)'
    AFTER `default_supplier_id`,
  ADD COLUMN `min_stock_qty`        DECIMAL(14,3)  NULL
    COMMENT 'Reorder alert threshold in base unit (R-INV-04 will use this)'
    AFTER `default_warehouse_id`;

-- 索引: 查询场景"按仓库列出物料"、"按子类做报表"
ALTER TABLE `input_item`
  ADD INDEX `idx_input_warehouse` (`default_warehouse_id`),
  ADD INDEX `idx_input_category_l2` (`category_l2`);

-- ============================================================================
-- 验证 (运行后)
-- ============================================================================
-- DESC input_item;
--   应看到 5 个新字段: category_l2, pack_qty, pack_unit_label, default_warehouse_id, min_stock_qty
--
-- SHOW INDEX FROM input_item WHERE Key_name IN ('idx_input_warehouse','idx_input_category_l2');
--   应看到 2 条
-- ============================================================================
