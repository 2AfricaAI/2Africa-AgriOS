-- ============================================================================
-- migration 050 ROLLBACK -- Sprint 51 Day 1
--
-- Hand-run script to fully revert migration 050. Not auto-applied.
-- Usage:
--   docker compose -f backend/docker-compose.yml exec -T mysql \
--       mysql -uroot -proot123456 toafrica_agrios \
--       < migrations/050_org_model_rollback.sql
--
-- This is destructive. Backup first if Sprint 51 onwards has already gone
-- to production and you have org_node / org_user rows you care about.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. Drop the node_id columns we added (idempotent helper)
-- ----------------------------------------------------------------------------

DELIMITER //
DROP PROCEDURE IF EXISTS migration_050_drop_column;
CREATE PROCEDURE migration_050_drop_column(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name   = p_table
           AND column_name  = p_column
    ) THEN
        SET @ddl := CONCAT('ALTER TABLE ', p_table, ' DROP COLUMN ', p_column);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL migration_050_drop_column('plot',             'node_id');
CALL migration_050_drop_column('activity',         'node_id');
CALL migration_050_drop_column('harvest_record',   'node_id');
CALL migration_050_drop_column('batch',            'node_id');
CALL migration_050_drop_column('planting_plan',    'node_id');
CALL migration_050_drop_column('customer',         'node_id');
CALL migration_050_drop_column('sales_order',      'node_id');
CALL migration_050_drop_column('purchase_order',   'node_id');
CALL migration_050_drop_column('cs_contact_link',  'node_id');
CALL migration_050_drop_column('cs_csat_response', 'node_id');
CALL migration_050_drop_column('complaint',        'node_id');
CALL migration_050_drop_column('qc_inspection',    'node_id');
CALL migration_050_drop_column('inventory',        'node_id');
CALL migration_050_drop_column('packing',          'node_id');
CALL migration_050_drop_column('revenue',          'node_id');

CALL migration_050_drop_column('sys_user', 'primary_node_id');
CALL migration_050_drop_column('sys_user', 'password_must_change');


-- ----------------------------------------------------------------------------
-- 2. Remove Kang account + bindings
-- ----------------------------------------------------------------------------

DELETE FROM org_user
 WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'kang.manager');

DELETE FROM sys_user_role
 WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'kang.manager');

DELETE FROM sys_user WHERE username = 'kang.manager';


-- ----------------------------------------------------------------------------
-- 3. Drop the 5 new tables (children first)
-- ----------------------------------------------------------------------------

DROP TABLE IF EXISTS data_access_audit;
DROP TABLE IF EXISTS org_node_tag;
DROP TABLE IF EXISTS org_user;
DROP TABLE IF EXISTS org_tag;
DROP TABLE IF EXISTS org_node;


-- ----------------------------------------------------------------------------
-- 4. Cleanup helper
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS migration_050_drop_column;


-- ============================================================================
-- End of rollback. Verify with:
--   SHOW TABLES LIKE 'org_%';                  -- expect empty
--   SHOW COLUMNS FROM plot LIKE 'node_id';     -- expect empty
--   SELECT COUNT(*) FROM sys_user WHERE username='kang.manager';   -- expect 0
-- ============================================================================
