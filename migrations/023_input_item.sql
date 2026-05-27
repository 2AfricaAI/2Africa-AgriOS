-- ============================================================================
-- Sprint 21.1 - Input Item master data (Phase 4)
--   Upgrades the old enum-only input_type into a full SKU master:
--   spec, active ingredient, PHI (Pre-Harvest Interval), registration #, supplier.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `input_item` (
  `id`                   BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                 VARCHAR(32)  NOT NULL UNIQUE COMMENT 'e.g. II-0001',
  `name`                 VARCHAR(128) NOT NULL COMMENT 'Display name (English-first for Kenya market)',
  `name_en`              VARCHAR(128) COMMENT 'Reserved for future bilingual support',
  `input_type`           VARCHAR(32)  NOT NULL
                         COMMENT 'fertilizer/pesticide/seed/film/labor/other',
  `spec`                 VARCHAR(128) COMMENT 'e.g. 50kg/bag, 1L/bottle',
  `unit`                 VARCHAR(16)  NOT NULL DEFAULT 'kg' COMMENT 'kg/L/pack/box/pcs',
  `active_ingredient`    VARCHAR(128) COMMENT 'Mainly for pesticides',
  `registration_no`      VARCHAR(64)  COMMENT 'Pesticide registration # (Kenya PCPB etc.)',
  `phi_days`             INT          NOT NULL DEFAULT 0
                         COMMENT 'Pre-Harvest Interval in days (pesticide only)',
  `default_supplier_id`  BIGINT       COMMENT 'FK -> supplier.id (optional)',
  `status`               VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `remark`               VARCHAR(255),
  `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `created_by`           BIGINT,
  `updated_at`           DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`           BIGINT,
  KEY `idx_input_type` (`input_type`),
  KEY `idx_default_supplier` (`default_supplier_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Input item master data (Phase 4)';

-- ============================================================================
-- Seed data - 5 common inputs in Kenya farming
--   NOTE: II-0003 (Glyphosate) is deliberately skipped - Kenya PCPB revoked
--         its registration (2024+). The gap in numbering serves as a reminder.
-- ============================================================================
INSERT INTO `input_item`
  (`code`, `name`, `input_type`, `spec`, `unit`, `active_ingredient`,
   `registration_no`, `phi_days`, `remark`)
VALUES
  ('II-0001', 'Urea 46-0-0',            'fertilizer', '50kg/bag', 'kg',
   'N 46%', NULL, 0,
   'Kenya - YARA mainstream nitrogen fertilizer'),
  ('II-0002', 'NPK 17-17-17',           'fertilizer', '50kg/bag', 'kg',
   'N 17%, P2O5 17%, K2O 17%', NULL, 0,
   'Base fertilizer for maize and coffee'),
  ('II-0004', 'Chlorantraniliprole 200g/L', 'pesticide', '250ml/bottle', 'L',
   'Chlorantraniliprole 200g/L', 'PCPB(CR)5678', 7,
   'Insecticide for avocado and maize pests - PHI 7 days'),
  ('II-0005', 'Maize Seed H614',        'seed',       '5kg/bag', 'kg',
   NULL, NULL, 0,
   'KARI hybrid - mainstream maize variety'),
  ('II-0006', 'Black Mulch Film',       'film',       '100m/roll', 'pcs',
   NULL, NULL, 0,
   'Weed suppression and water retention');
