-- ============================================================================
-- migration 046: built-in role → menu bindings
--
-- Why this exists
--   Migration 039_perms_seed.sql ships the canonical role-to-menu mapping
--   for the five non-admin built-in roles (MANAGER / LEADER / PACKHOUSE /
--   SALES / WORKER). It assumes those role rows already exist in sys_role.
--
--   On a v3.0-era schema.sql + migrations 009-038 path, those rows came
--   from a backup-restored admin database — so 039 ran fine. On a fresh
--   `git clone + docker compose up` install however, the role rows do not
--   exist until migration 044 creates them, which runs AFTER 039 (numeric
--   order). Result: 039's INSERTs select 0 rows and silently do nothing,
--   and the five non-admin built-in roles are left with empty menu
--   bindings — they can log in but see no business menus.
--
-- What this migration does
--   Re-runs the menu-binding INSERTs from 039 with `INSERT IGNORE`
--   semantics, now that 044 has populated the role rows. Idempotent —
--   safe to re-run on a database that already has these bindings.
--
-- Permission flow
--   AgriOS stores permission strings on sys_menu.perms. The JWT auth
--   filter inflates a user's perm set by joining sys_user_role →
--   sys_role_menu → sys_menu. So binding a role to a menu is also what
--   binds the role to that menu's perms — there is no separate
--   sys_role_perm table.
--
-- Origin
--   The selection SQL below is a verbatim copy of migrations/039_perms_seed.sql.
--   When 039 is next edited, this migration should be edited in lockstep.
-- ============================================================================


-- ---------- MANAGER: everything except System (parent dir 11) ----------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'MANAGER'
   AND m.id <> 11
   AND m.parent_id <> 11
   AND NOT EXISTS (SELECT 1 FROM sys_menu p WHERE p.id = m.parent_id AND p.parent_id = 11);


-- ---------- LEADER: production + harvests + warehouse view + QC + action ----
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'LEADER'
   AND (
        m.id IN (1, 3, 4, 5, 8)
     OR m.parent_id IN (3, 8)
     OR m.code LIKE 'production.%'
     OR m.code LIKE 'operations.actionBoard%'
     OR m.code IN ('warehouse.inbound','warehouse.outbound','warehouse.reports','qc.inspections','qc.trace')
   );


-- ---------- PACKHOUSE: packhouse + inventory + QC inspections + home --------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'PACKHOUSE'
   AND (
        m.id IN (1, 5, 6)
     OR m.parent_id = 6
     OR m.code LIKE 'packhouse.%'
     OR m.code LIKE 'qc.inspections%'
     OR m.code = 'qc.trace'
   );


-- ---------- SALES: customers + orders + AR + home ---------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'SALES'
   AND (
        m.id IN (1, 7, 9)
     OR m.parent_id = 7
     OR m.code LIKE 'sales.%'
     OR m.code LIKE 'finance.ar%'
     OR m.code = 'finance.reports'
   );


-- ---------- WORKER: home + production list-only access ----------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'WORKER'
   AND (
        m.id IN (1, 3)
     OR m.code IN ('production.plots','production.activities','production.harvests',
                   'production.activities.add','production.harvests.add')
   );
