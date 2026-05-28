-- ============================================================================
-- Sprint 36: add is_built_in flag to sys_role so the UI can distinguish
-- preset (immutable) roles from custom roles created by SUPER_ADMIN.
--
--   is_built_in = 1 -> seeded role, the Edit menus dialog locks all access
--                     levels and Delete is disabled.
--   is_built_in = 0 -> custom role, fully editable.
-- ============================================================================

ALTER TABLE sys_role
    ADD COLUMN is_built_in TINYINT(1) NOT NULL DEFAULT 0
    AFTER data_scope;

-- Mark the six seeded roles as built-in
UPDATE sys_role
   SET is_built_in = 1
 WHERE code IN ('SUPER_ADMIN','MANAGER','LEADER','PACKHOUSE','SALES','WORKER');
