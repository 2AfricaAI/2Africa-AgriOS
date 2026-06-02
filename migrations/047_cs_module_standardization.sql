-- ============================================================================
-- migration 047: CS module standardization (Sprint 48a)
--
-- Background:
--   The "service module" (Sprint 40-47) was built as an AgriOS-specific
--   integration with Chatwoot. We are now promoting it to a horizontal
--   "Customer Service Core" (CS-Core) module that other 2Africa products
--   (RetailOS / FactoryOS / TravelOS / AgriCloud / MarketOS) will adopt.
--
--   To make the schema portable, this migration:
--     1) renames `service_*` tables to `cs_*`
--     2) renames `agrios_entity_*` columns to generic `subject_*`
--     3) preserves all data, indexes, and FKs
--
--   Each consuming product implements a BusinessContextProvider in Java
--   that maps cs_contact_link.subject_type='customer' (or 'buyer' /
--   'traveler' / ...) to its own domain entity.
--
-- Migration safety:
--   - Idempotent: uses IF EXISTS / dynamic SQL guards so re-running is OK
--   - Backward-compat: legacy table names DROPPED, callers updated in same PR
--   - No data loss: RENAME TABLE preserves rows + indexes; CHANGE COLUMN
--     keeps existing data unchanged in column rename
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Stored procedure helper: rename only if old table exists AND new table
-- does not. MySQL has no IF EXISTS for RENAME TABLE, so we DIY guard it.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cs_rename_if_needed;
DELIMITER //
CREATE PROCEDURE cs_rename_if_needed(IN old_name VARCHAR(64), IN new_name VARCHAR(64))
BEGIN
    DECLARE old_exists INT DEFAULT 0;
    DECLARE new_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = old_name;
    SELECT COUNT(*) INTO new_exists FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = new_name;
    IF old_exists = 1 AND new_exists = 0 THEN
        SET @sql = CONCAT('RENAME TABLE `', old_name, '` TO `', new_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ----------------------------------------------------------------------------
-- Stored procedure helper: rename column only if old column exists AND new
-- column does not. Same guard logic for idempotency.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cs_rename_column_if_needed;
DELIMITER //
CREATE PROCEDURE cs_rename_column_if_needed(
    IN tbl_name VARCHAR(64),
    IN old_col VARCHAR(64),
    IN new_col VARCHAR(64),
    IN col_def VARCHAR(255)
)
BEGIN
    DECLARE old_exists INT DEFAULT 0;
    DECLARE new_exists INT DEFAULT 0;
    DECLARE tbl_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = tbl_name;
    IF tbl_exists = 1 THEN
        SELECT COUNT(*) INTO old_exists FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = tbl_name
            AND column_name = old_col;
        SELECT COUNT(*) INTO new_exists FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = tbl_name
            AND column_name = new_col;
        IF old_exists = 1 AND new_exists = 0 THEN
            SET @sql = CONCAT(
              'ALTER TABLE `', tbl_name, '` CHANGE `', old_col, '` `', new_col, '` ', col_def
            );
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //
DELIMITER ;

-- ----------------------------------------------------------------------------
-- Stored procedure helper: drop an index if it exists.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cs_drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE cs_drop_index_if_exists(IN tbl_name VARCHAR(64), IN idx_name VARCHAR(64))
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_exists FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = tbl_name AND index_name = idx_name;
    IF idx_exists = 1 THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` DROP INDEX `', idx_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ----------------------------------------------------------------------------
-- Stored procedure helper: add an index if it does NOT exist.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cs_add_index_if_missing;
DELIMITER //
CREATE PROCEDURE cs_add_index_if_missing(
    IN tbl_name VARCHAR(64),
    IN idx_name VARCHAR(64),
    IN idx_def VARCHAR(500)
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_exists FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = tbl_name AND index_name = idx_name;
    IF idx_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD INDEX `', idx_name, '` ', idx_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ============================================================================
-- 1. service_contact_link  ->  cs_contact_link
--    agrios_entity_type    ->  subject_type
--    agrios_entity_id      ->  subject_id
--    Rebuild the idx_agrios_entity index as idx_subject
-- ============================================================================
CALL cs_rename_if_needed('service_contact_link', 'cs_contact_link');
CALL cs_rename_column_if_needed(
    'cs_contact_link', 'agrios_entity_type', 'subject_type',
    'VARCHAR(32) NOT NULL DEFAULT ''customer'' COMMENT ''customer / supplier / partner / buyer / traveler / etc.'''
);
CALL cs_rename_column_if_needed(
    'cs_contact_link', 'agrios_entity_id', 'subject_id',
    'BIGINT NOT NULL COMMENT ''id of the row in the consuming product''''s entity table'''
);
CALL cs_drop_index_if_exists('cs_contact_link', 'idx_agrios_entity');
CALL cs_add_index_if_missing('cs_contact_link', 'idx_subject', '(subject_type, subject_id)');

-- ============================================================================
-- 2. service_event_log  ->  cs_event_log
--    agrios_entity_type ->  subject_type
--    agrios_entity_id   ->  subject_id
--    Rebuild idx_agrios_entity as idx_subject
-- ============================================================================
CALL cs_rename_if_needed('service_event_log', 'cs_event_log');
CALL cs_rename_column_if_needed(
    'cs_event_log', 'agrios_entity_type', 'subject_type',
    'VARCHAR(32) NULL COMMENT ''customer / supplier / partner / buyer / etc.'''
);
CALL cs_rename_column_if_needed(
    'cs_event_log', 'agrios_entity_id', 'subject_id',
    'BIGINT NULL COMMENT ''id of the row in the consuming product''''s entity table'''
);
CALL cs_drop_index_if_exists('cs_event_log', 'idx_agrios_entity');
CALL cs_add_index_if_missing('cs_event_log', 'idx_subject', '(subject_type, subject_id, created_at)');

-- ============================================================================
-- Update table comments to reflect cross-product CS-Core positioning
-- ============================================================================
SET @stmt = (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cs_contact_link'),
    'ALTER TABLE cs_contact_link COMMENT = ''CS-Core: bridges a consuming-product entity (customer / buyer / traveler / ...) to a Chatwoot Contact''',
    'SELECT 1'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt = (SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cs_event_log'),
    'ALTER TABLE cs_event_log COMMENT = ''CS-Core: cross-system event audit log (inbound webhooks, outbound actions, sync jobs, AI decisions)''',
    'SELECT 1'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------------------------------------------------------
-- Clean up the helper procedures
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cs_rename_if_needed;
DROP PROCEDURE IF EXISTS cs_rename_column_if_needed;
DROP PROCEDURE IF EXISTS cs_drop_index_if_exists;
DROP PROCEDURE IF EXISTS cs_add_index_if_missing;
