-- ============================================================================
-- Migration 027: purchase_order_item 增加 input_item_id 软外键
-- Sprint 22.1a — PO 明细行打通到 input_item 主数据
-- ============================================================================
-- 目的:
--   把 purchase_order_item.description (自由文本) 升级为引用
--   input_item 主数据,避免同一物料在不同 PO 上名称写法不一致。
--   保留 description / input_type 作为冗余快照,便于历史回溯。
--
-- 设计原则:
--   - 软外键 (BIGINT NULL),不加 FK 约束,允许历史数据 NULL
--   - 新建 PO 明细时必须选择 input_item_id (前端校验)
--   - description 仍保留,作为下单时的"快照名称"(后续主数据改名不影响历史 PO)
--   - input_type 同理保留快照,后续可与 input_item.category 比对一致性
-- ============================================================================

USE toafrica_agrios;

-- 1) 加列
ALTER TABLE `purchase_order_item`
  ADD COLUMN `input_item_id` BIGINT NULL
    COMMENT 'FK -> input_item.id (soft FK; legacy rows may be null)'
  AFTER `po_id`;

-- 2) 索引(配合后续报表查询: 按物料统计采购量/采购成本)
ALTER TABLE `purchase_order_item`
  ADD INDEX `idx_input_item` (`input_item_id`);

-- ============================================================================
-- 验证 (运行后请执行)
-- ============================================================================
-- DESC purchase_order_item;
--   应看到 input_item_id BIGINT NULL 在 po_id 之后
--
-- SHOW INDEX FROM purchase_order_item WHERE Key_name = 'idx_input_item';
--   应有一条记录
-- ============================================================================
