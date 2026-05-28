-- ============================================================================
-- Sprint 11 - 投入品成本 / Activity 成本字段
--   V2.0 Phase 2 P&L 的成本侧种子数据。配合 revenue 表(已建)能算 SKU/Plot 毛利。
-- ============================================================================

-- 1. activity_input: 加货币 (cost amount 已经存在)
ALTER TABLE `activity_input`
  ADD COLUMN `currency` VARCHAR(8) NOT NULL DEFAULT 'KES'
  COMMENT 'KES / USD / EUR'
  AFTER `cost`;

-- 2. activity: 加人工 / 水电 / 其他成本 + 货币
ALTER TABLE `activity`
  ADD COLUMN `labor_cost`     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Labor cost' AFTER `remark`,
  ADD COLUMN `utility_cost`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Utility cost' AFTER `labor_cost`,
  ADD COLUMN `other_cost`     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Other cost' AFTER `utility_cost`,
  ADD COLUMN `cost_currency`  VARCHAR(8)    NOT NULL DEFAULT 'KES' COMMENT 'Currency for labor/utility/other generic costs' AFTER `other_cost`;
