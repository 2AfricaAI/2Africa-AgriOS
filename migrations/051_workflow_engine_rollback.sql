-- ============================================================================
-- migration 051 ROLLBACK -- Sprint 52 Day 1
-- ============================================================================

-- 1. Drop the immutability triggers first (they would block table drops)
DROP TRIGGER IF EXISTS wf_audit_no_update;
DROP TRIGGER IF EXISTS wf_audit_no_delete;

-- 2. Delete permission bindings + menu rows
DELETE FROM sys_role_menu WHERE menu_id IN (990, 991, 992, 993, 994);
DELETE FROM sys_menu     WHERE id     IN (990, 991, 992, 993, 994);

-- 3. Drop tables (children first; no FK so order is just for clarity)
DROP TABLE IF EXISTS wf_audit;
DROP TABLE IF EXISTS wf_delegation;
DROP TABLE IF EXISTS wf_step;
DROP TABLE IF EXISTS wf_instance;
DROP TABLE IF EXISTS wf_definition;

-- ============================================================================
-- Verify:
--   SHOW TABLES LIKE 'wf_%';                  -- empty
--   SELECT * FROM sys_menu WHERE id BETWEEN 990 AND 994;   -- empty
-- ============================================================================
