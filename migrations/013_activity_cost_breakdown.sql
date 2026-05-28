-- ============================================================================
-- Sprint 11.1 - 细分 activity 成本字段
--   把笼统的 utility_cost 拆成 water_cost + electricity_cost,新增 fertilizer_cost
-- ============================================================================

ALTER TABLE `activity`
  ADD COLUMN `water_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Water cost' AFTER `labor_cost`,
  ADD COLUMN `electricity_cost` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Electricity cost' AFTER `water_cost`,
  ADD COLUMN `fertilizer_cost`  DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Fertilizer cost' AFTER `electricity_cost`,
  DROP COLUMN `utility_cost`;
