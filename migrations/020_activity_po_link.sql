-- ============================================================================
-- Sprint 17.7 - Activity 关联 PO 行
--   让 cost 字段可以追溯到来源 purchase_order_item, 形成"票据驱动成本"闭环.
--   都是 nullable: 老数据继续手填 amount 不影响.
--   每个 cost 字段配一个 *_po_item_id FK (软外键, 仅 INDEX 不加 FK 约束)
-- ============================================================================

ALTER TABLE `activity`
  ADD COLUMN `labor_po_item_id`       BIGINT NULL COMMENT '关联人工 PO 行'  AFTER `labor_cost`,
  ADD COLUMN `water_po_item_id`       BIGINT NULL COMMENT '关联水费 PO 行'  AFTER `water_cost`,
  ADD COLUMN `electricity_po_item_id` BIGINT NULL COMMENT '关联电费 PO 行'  AFTER `electricity_cost`,
  ADD COLUMN `fertilizer_po_item_id`  BIGINT NULL COMMENT '关联肥料 PO 行'  AFTER `fertilizer_cost`,
  ADD COLUMN `other_po_item_id`       BIGINT NULL COMMENT '关联其他 PO 行'  AFTER `other_cost`;

CREATE INDEX `idx_activity_fertilizer_po` ON `activity`(`fertilizer_po_item_id`);
CREATE INDEX `idx_activity_labor_po`      ON `activity`(`labor_po_item_id`);
