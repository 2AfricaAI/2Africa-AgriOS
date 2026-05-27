-- ============================================================================
-- Sprint 22.0 - Warehouse purpose + Input categories (Phase 4)
--
-- DESIGN: GAP compliance + Kenya farm reality require strict separation of
-- finished goods vs inputs, and within inputs further separation by category
-- (seeds / fertilizers / pesticides / construction / spare parts / tools /
-- packaging). Mixing pesticides with food in storage breaks export audit and
-- exposes workers to safety risk.
--
-- All values English-only per Sprint 21+ convention.
-- ============================================================================

-- 1. Add purpose column to location_warehouse (existing rows default to finished_goods)
ALTER TABLE `location_warehouse`
  ADD COLUMN `purpose` VARCHAR(32) NOT NULL DEFAULT 'finished_goods'
  COMMENT 'finished_goods | seed_storage | fertilizer_storage | pesticide_storage | construction_storage | spare_parts_storage | tools_storage | packaging_storage | other_storage'
  AFTER `type`;

CREATE INDEX `idx_warehouse_purpose` ON `location_warehouse` (`purpose`);

-- 2. Seed 7 input-side warehouses (W01/W02 already exist as finished_goods default)
--    parent_id=0 means top-level (no parent shelf grouping for now)
INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `parent_id`) VALUES
  ('IW-SEED', 'Seed Storage',          'normal', 'seed_storage',         0),
  ('IW-FERT', 'Fertilizer Storage',    'normal', 'fertilizer_storage',   0),
  ('IW-PEST', 'Pesticide Locker',      'normal', 'pesticide_storage',    0),
  ('IW-CON',  'Construction Yard',     'normal', 'construction_storage', 0),
  ('IW-PART', 'Spare Parts Cabinet',   'normal', 'spare_parts_storage',  0),
  ('IW-TOOL', 'Tool Room',             'normal', 'tools_storage',        0),
  ('IW-PKG',  'Packaging Materials',   'normal', 'packaging_storage',    0)
ON DUPLICATE KEY UPDATE name=VALUES(name), purpose=VALUES(purpose);

-- 3. Remap existing input_item.input_type 'film' -> 'construction'
--    (film like greenhouse mulch belongs under construction materials)
UPDATE `input_item` SET `input_type` = 'construction' WHERE `input_type` = 'film';

-- 4. (Documentary) The expanded input_type enum (enforced at backend @Pattern):
--    seed | fertilizer | pesticide | construction | spare_parts | tools | packaging | other
--
-- The pairing rule (enforced in service layer when stock movement happens):
--    seed         -> seed_storage
--    fertilizer   -> fertilizer_storage
--    pesticide    -> pesticide_storage
--    construction -> construction_storage
--    spare_parts  -> spare_parts_storage
--    tools        -> tools_storage
--    packaging    -> packaging_storage
--    other        -> other_storage
--
-- Finished SKU packing goes into purpose='finished_goods' (PackingService check).
