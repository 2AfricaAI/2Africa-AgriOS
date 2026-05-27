-- ============================================================================
-- Sprint 22.0.7 - Adopt design proposal coding convention (Phase 4)
--
-- Per "农场仓库管理设计方案 V1.0" §17:
--   AGW = Agricultural Warehouse (inputs)
--   TLW = Tools & Spare Parts Warehouse
--   CLD = Cold storage (finished goods)
--   FRZ = Frozen storage
--   PKH = Packing House
--   RTN = Returns area
--
-- This migration ONLY renames codes - schema and purpose values stay the same.
-- Field-readable, scan-friendly, label-printable codes.
-- ============================================================================

-- Finished goods (top-level)
UPDATE `location_warehouse` SET `code` = 'CLD-MAIN', `name` = 'Cold Storage - Main'  WHERE `code` = 'W01';
UPDATE `location_warehouse` SET `code` = 'CLD-COLD', `name` = 'Cold Storage - Frozen' WHERE `code` = 'W02';

-- Pre-existing sub-shelves of W01/W02 (if present from initial seed)
UPDATE `location_warehouse` SET `code` = 'CLD-MAIN-A1' WHERE `code` = 'W01-A1';
UPDATE `location_warehouse` SET `code` = 'CLD-MAIN-A2' WHERE `code` = 'W01-A2';
UPDATE `location_warehouse` SET `code` = 'CLD-COLD-C1' WHERE `code` = 'W02-C1';

-- Input warehouses (top-level)
UPDATE `location_warehouse` SET `code` = 'AGW-SEE', `name` = 'Seed Storage'           WHERE `code` = 'IW-SEED';
UPDATE `location_warehouse` SET `code` = 'AGW-FER', `name` = 'Fertilizer Storage'     WHERE `code` = 'IW-FERT';
UPDATE `location_warehouse` SET `code` = 'AGW-CHE', `name` = 'Chemical / Pesticide Locker' WHERE `code` = 'IW-PEST';
UPDATE `location_warehouse` SET `code` = 'AGW-CON', `name` = 'Construction Yard'      WHERE `code` = 'IW-CON';
UPDATE `location_warehouse` SET `code` = 'TLW-SPT', `name` = 'Spare Parts Cabinet'    WHERE `code` = 'IW-PART';
UPDATE `location_warehouse` SET `code` = 'TLW-TOL', `name` = 'Tool Room'              WHERE `code` = 'IW-TOOL';
UPDATE `location_warehouse` SET `code` = 'AGW-PKG', `name` = 'Packaging Materials'    WHERE `code` = 'IW-PKG';

-- IW-FERT sub-nodes (zones + shelf + bins)
UPDATE `location_warehouse` SET `code` = 'AGW-FER-A',     `name` = 'Bulk Zone A (Urea)' WHERE `code` = 'IW-FERT-A';
UPDATE `location_warehouse` SET `code` = 'AGW-FER-B',     `name` = 'Bulk Zone B (NPK)'  WHERE `code` = 'IW-FERT-B';
UPDATE `location_warehouse` SET `code` = 'AGW-FER-S1',    `name` = 'Sample Shelf S1'    WHERE `code` = 'IW-FERT-S1';
UPDATE `location_warehouse` SET `code` = 'AGW-FER-S1-01', `name` = 'Bin 01'             WHERE `code` = 'IW-FERT-S1-01';
UPDATE `location_warehouse` SET `code` = 'AGW-FER-S1-02', `name` = 'Bin 02'             WHERE `code` = 'IW-FERT-S1-02';

-- After this migration, expected codes:
--   CLD-MAIN, CLD-COLD       finished_goods
--   CLD-MAIN-A1/A2, CLD-COLD-C1  (sub-shelves)
--   AGW-SEE   seed_storage
--   AGW-FER   fertilizer_storage       (+ -A/-B/-S1/-S1-01/-S1-02)
--   AGW-CHE   pesticide_storage
--   AGW-CON   construction_storage
--   AGW-PKG   packaging_storage
--   TLW-SPT   spare_parts_storage
--   TLW-TOL   tools_storage
