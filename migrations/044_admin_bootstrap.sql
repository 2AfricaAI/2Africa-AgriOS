-- ============================================================================
-- migration 044: bootstrap roles + default admin user
--
-- Why this exists
--   The Sprint 38 release relied on a backup-restored admin row being present
--   in sys_user. A clean install (fresh MySQL volume + schema.sql + migrations
--   037–043) has the schema in place but no admin user and no built-in roles,
--   so logging in is impossible and every permission-gated menu is hidden.
--
-- What this migration does
--   1. Inserts the 6 built-in roles (SUPER_ADMIN / MANAGER / LEADER /
--      PACKHOUSE / SALES / WORKER) if missing. Codes match what
--      migration 039_perms_seed.sql later assumes when wiring perms.
--   2. Gives SUPER_ADMIN every existing sys_menu row (mirrors the
--      CROSS JOIN already used by 039).
--   3. Creates a built-in `admin` user with password `Admin@123456`
--      (BCrypt hash, cost 12).
--   4. Binds admin → SUPER_ADMIN.
--
-- All inserts are idempotent — re-running this migration is safe and a no-op
-- once the rows exist. INSERT IGNORE / ON DUPLICATE KEY UPDATE clauses make
-- sure existing data is preserved.
--
-- Operators rolling this out to a real customer SHOULD change the admin
-- password immediately via /v1/auth/change-password (Sprint 42 will add a
-- "first-login forced password change" prompt).
-- ============================================================================


-- ---------- 1. Built-in roles ----------------------------------------------
-- Column shape matches the migration 042_partner_roles_and_perms.sql pattern:
--   (code, name, data_scope, is_built_in, remark)
-- data_scope semantics:
--   self  -> sees only own data
--   group -> sees same-group data
--   all   -> sees everything in the account
INSERT INTO sys_role (code, name, data_scope, is_built_in, remark)
VALUES
  ('SUPER_ADMIN', 'Super Admin',  'all',   1, 'Built-in: full access to every menu and perm'),
  ('MANAGER',     'Manager',      'all',   1, 'Built-in: senior operator, everything except System'),
  ('LEADER',      'Team Leader',  'group', 1, 'Built-in: production + harvests + warehouse view + QC'),
  ('PACKHOUSE',   'Packhouse',    'group', 1, 'Built-in: packhouse + inventory + QC inspections'),
  ('SALES',       'Sales',        'group', 1, 'Built-in: customers + orders + AR'),
  ('WORKER',      'Field Worker', 'self',  1, 'Built-in: mobile-only, list-only access')
ON DUPLICATE KEY UPDATE
  name        = VALUES(name),
  data_scope  = VALUES(data_scope),
  is_built_in = VALUES(is_built_in),
  remark      = VALUES(remark);


-- ---------- 2. SUPER_ADMIN sees every menu ---------------------------------
-- Mirrors the CROSS JOIN used in migration 039_perms_seed.sql.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  CROSS JOIN sys_menu m
 WHERE r.code = 'SUPER_ADMIN';


-- ---------- 3. Default admin user ------------------------------------------
-- BCrypt cost 12 of "Admin@123456".
-- Rotate immediately after first login on any production deployment.
INSERT INTO sys_user (username, password, nickname, status, created_at)
VALUES (
  'admin',
  '$2b$12$h56Q.mHc8Km2Oy9BlbLd0evBG8YBqY1vAKEfXDM4x6xkuKRAMiNu2',
  'Administrator',
  'active',
  NOW()
)
ON DUPLICATE KEY UPDATE
  password = IF(password IS NULL OR password = '', VALUES(password), password),
  status   = 'active';


-- ---------- 4. Bind admin -> SUPER_ADMIN ------------------------------------
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
  FROM sys_user u, sys_role r
 WHERE u.username = 'admin'
   AND r.code     = 'SUPER_ADMIN';
