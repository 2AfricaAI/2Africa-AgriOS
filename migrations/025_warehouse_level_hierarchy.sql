-- ============================================================================
-- Sprint 22.0.5 - Warehouse hierarchy level (Phase 4)
--
-- DESIGN: 4-level enum captures both floor-stack and rack patterns.
--   warehouse: top container (cannot directly hold stock; must have children)
--   zone:      area inside warehouse (floor-stack works here, e.g. bulk fertilizer)
--   shelf:     rack inside warehouse or zone (small items)
--   bin:       single slot on a shelf (leaf, fine-grained position)
--
-- RULE (enforced in service layer when input_stock is built):
--   Stock can only attach to LEAF nodes (no children).
--   - A leaf zone = floor-stack location.
--   - A leaf shelf = whole rack treated as one bucket.
--   - A leaf bin = a specific slot.
--   - A warehouse can NEVER be a leaf (must have children before stocking).
--
-- All values English-only.
-- ============================================================================

ALTER TABLE `location_warehouse`
  ADD COLUMN `level` VARCHAR(16) NOT NULL DEFAULT 'warehouse'
  COMMENT 'Hierarchy level: warehouse | zone | shelf | bin'
  AFTER `purpose`;

CREATE INDEX `idx_warehouse_level` ON `location_warehouse` (`level`);

-- Retrofit: 9 existing rows (W01/W02/IW-*) are top-level warehouses
UPDATE `location_warehouse` SET `level` = 'warehouse' WHERE `parent_id` = 0 OR `parent_id` IS NULL;

-- ============================================================================
-- Demo children: IW-FERT mixed (Path D) - bulk zones + small-item shelf+bins
--   IW-FERT-A    Zone A (floor stack for Urea)
--   IW-FERT-B    Zone B (floor stack for NPK)
--   IW-FERT-S1   Shelf S1 (small sample bags)
--     IW-FERT-S1-01  Bin
--     IW-FERT-S1-02  Bin
-- ============================================================================
INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `level`, `parent_id`)
  SELECT 'IW-FERT-A',  'Bulk Zone A (Urea)',     'normal', 'fertilizer_storage', 'zone',
         (SELECT id FROM (SELECT id FROM location_warehouse WHERE code='IW-FERT') x)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `level`, `parent_id`)
  SELECT 'IW-FERT-B',  'Bulk Zone B (NPK)',      'normal', 'fertilizer_storage', 'zone',
         (SELECT id FROM (SELECT id FROM location_warehouse WHERE code='IW-FERT') x)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `level`, `parent_id`)
  SELECT 'IW-FERT-S1', 'Sample Shelf S1',        'normal', 'fertilizer_storage', 'shelf',
         (SELECT id FROM (SELECT id FROM location_warehouse WHERE code='IW-FERT') x)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `level`, `parent_id`)
  SELECT 'IW-FERT-S1-01', 'Bin 01',              'normal', 'fertilizer_storage', 'bin',
         (SELECT id FROM (SELECT id FROM location_warehouse WHERE code='IW-FERT-S1') x)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `location_warehouse` (`code`, `name`, `type`, `purpose`, `level`, `parent_id`)
  SELECT 'IW-FERT-S1-02', 'Bin 02',              'normal', 'fertilizer_storage', 'bin',
         (SELECT id FROM (SELECT id FROM location_warehouse WHERE code='IW-FERT-S1') x)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ============================================================================
-- After migration, IW-FERT looks like:
--   IW-FERT (warehouse)             <- NOT a leaf, cannot hold stock
--     IW-FERT-A      (zone, leaf)   <- floor-stack: Urea ✓ stockable
--     IW-FERT-B      (zone, leaf)   <- floor-stack: NPK  ✓ stockable
--     IW-FERT-S1     (shelf)        <- NOT a leaf
--       IW-FERT-S1-01 (bin, leaf)   ✓ stockable
--       IW-FERT-S1-02 (bin, leaf)   ✓ stockable
-- ============================================================================
