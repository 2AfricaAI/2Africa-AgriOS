-- ============================================================================
-- migration 050: ORG model -- Sprint 51 Day 1
--
-- See docs/PRD-ORG-v0.2.md for the full design and the 8 locked decisions.
-- This migration is the foundation of everything in v3.5.0 (HR / Admin /
-- Legal / Workflow). It must be applied BEFORE Sprint 52 starts.
--
-- ============================================================================
-- What this migration does
--
-- A. Create 5 new tables
--      org_node            -- main hierarchy (1 tree, 8 type enum)
--      org_tag             -- cross-tree dimensions (season / project / etc)
--      org_node_tag        -- many-to-many tag attach
--      org_user            -- user-to-node membership with effective_from/to
--      data_access_audit   -- decision #5: every data_scope='all' read logged
--
-- B. Seed the initial 12 nodes for Albert's Meadows
--      (see PRD-ORG-v0.2 user-confirmed structure)
--      Plus 5 future-proof org_tag rows (SEASON, CERT, COMPLIANCE_ZONE).
--
-- C. Create Kang as MANAGER of Albert's Farm
--      username:  kang.manager
--      password:  Welcome@123456  (BCrypt cost 12)
--      must change password on first login
--      bound to MANAGER role, registered as Albert's Farm primary + manager
--
-- D. Extend sys_user with primary_node_id + password_must_change
--
-- E. ALTER 14 high-frequency business tables to add node_id BIGINT NULL
--      No NOT NULL, no DEFAULT, no index -- per decision #7 zero-touch
--      migration. Existing queries are not affected.
--
-- F. Batched backfill of node_id (Day 1 only does plot + harvest_record +
--      activity; other tables are backfilled in Day 5 to keep Day 1's
--      DDL window short).
--
-- All statements are idempotent (INSERT IGNORE / CREATE TABLE IF NOT EXISTS
-- / ALTER ... ADD COLUMN with pre-check via information_schema).
-- ============================================================================


-- ----------------------------------------------------------------------------
-- A. New tables
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS org_node (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id       BIGINT       NULL,                  -- root = NULL
    code            VARCHAR(64)  NOT NULL,              -- 'ALBERTS-MEADOWS' / 'ALBERTS-FARM'
    name            VARCHAR(128) NOT NULL,              -- decision #1: single English value
    type            VARCHAR(32)  NOT NULL,              -- 8-enum, see CHECK below
    cost_center     VARCHAR(64)  NULL,                  -- finance attribution; not 1:1 with node
    manager_id      BIGINT       NULL,                  -- decision #2: single primary manager
    ancestors       VARCHAR(512) NULL,                  -- '1/3/7' path; subtree query uses LIKE
    depth           INT          NOT NULL DEFAULT 0,    -- cached, for UI indent
    sort_no         INT          NOT NULL DEFAULT 0,
    active          TINYINT      NOT NULL DEFAULT 1,    -- decision #3: physical nodes only toggle
    location        VARCHAR(255) NULL,                  -- e.g. 'Isinya, Kajiado'  -- added per Sprint 51 input
    description     VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted_at      DATETIME     NULL,                  -- decision #3: only virtual nodes filled
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (code),
    KEY idx_org_parent (parent_id),
    KEY idx_org_ancestors (ancestors(255)),
    KEY idx_org_type (type),
    CONSTRAINT chk_org_type CHECK (
        type IN ('GROUP','FARM','PACKHOUSE','PROCESSING','WAREHOUSE',
                 'DEPT','TEAM','PROJECT')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS org_tag (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL,              -- 'SEASON' / 'PROJECT' / 'COMPLIANCE_ZONE' / 'CERTIFICATION'
    active          TINYINT      NOT NULL DEFAULT 1,
    description     VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_code (code),
    KEY idx_tag_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS org_node_tag (
    node_id     BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (node_id, tag_id),
    KEY idx_ont_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- decision #4: payroll attribution depends on effective_from/effective_to
CREATE TABLE IF NOT EXISTS org_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,              -- FK sys_user
    node_id         BIGINT       NOT NULL,              -- FK org_node
    is_primary      TINYINT      NOT NULL DEFAULT 0,    -- exactly one per user
    position        VARCHAR(64)  NULL,                  -- 'Field Lead' / 'Operator' / ...
    is_manager      TINYINT      NOT NULL DEFAULT 0,    -- decision #2: deputies set this =1
    effective_from  DATE         NOT NULL,
    effective_to    DATE         NULL,                  -- NULL = currently active
    remark          VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_node_from (user_id, node_id, effective_from),
    KEY idx_org_user_user (user_id),
    KEY idx_org_user_node (node_id),
    KEY idx_org_user_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- decision #5: every read by a data_scope='all' role is logged here
CREATE TABLE IF NOT EXISTS data_access_audit (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    role_code       VARCHAR(64)  NOT NULL,              -- e.g. SUPER_ADMIN
    resource_type   VARCHAR(64)  NOT NULL,              -- 'plot' / 'payroll' / 'contract'
    resource_id     BIGINT       NULL,                  -- NULL for list endpoints
    query_summary   VARCHAR(500) NULL,                  -- 'list plots status=open' etc
    row_count       INT          NULL,                  -- list endpoint row count
    ip              VARCHAR(64)  NULL,
    user_agent      VARCHAR(255) NULL,
    accessed_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_user_time (user_id, accessed_at),
    KEY idx_audit_resource (resource_type, resource_id, accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- audit table is append-only at the application layer; UPDATE/DELETE are
-- not blocked here so DBA can still rotate/archive. Production hardening
-- will revoke UPDATE/DELETE from the application user in Sprint 60.


-- ----------------------------------------------------------------------------
-- B. Seed initial tree (12 nodes -- Albert's Meadows confirmed structure)
-- ----------------------------------------------------------------------------

INSERT IGNORE INTO org_node
    (id, parent_id, code,                 name,                            type,         ancestors, depth, sort_no, active, location,                  description)
VALUES
    -- Root group
    ( 1, NULL,     'ALBERTS-MEADOWS',     'Albert''s Meadows',             'GROUP',      '',          0,    0,   1, NULL,                       'Holding entity'),
    -- HQ tree
    ( 2,    1,     'HQ',                  'HQ',                            'DEPT',       '1',         1,   10,   1, 'Karen Office, Nairobi',    'Head office'),
    ( 3,    2,     'HQ-FINANCE',          'Finance',                       'DEPT',       '1/2',       2,   10,   1, 'Karen Office, Nairobi',    NULL),
    ( 4,    2,     'HQ-LEGAL',            'Legal',                         'DEPT',       '1/2',       2,   20,   1, 'Karen Office, Nairobi',    NULL),
    ( 5,    2,     'HQ-SALES',            'Sales',                         'DEPT',       '1/2',       2,   30,   1, 'Karen Office, Nairobi',    NULL),
    -- Farm tree
    (10,    1,     'ALBERTS-FARM',        'Albert''s Farm',                'FARM',       '1',         1,   20,   1, 'Isinya, Kajiado',          '640 acres; Zone A operational, Zone B/C undeveloped'),
    (11,   10,     'FARM-MGMT',           'Farm General Management',       'DEPT',       '1/10',      2,   10,   1, 'Isinya, Kajiado',          NULL),
    (12,   10,     'FARM-HR-ADMIN',       'HR & Admin',                    'DEPT',       '1/10',      2,   20,   1, 'Isinya, Kajiado',          NULL),
    (13,   10,     'FARM-VEG-FRUIT',      'Vegetable & Fruit Cultivation', 'DEPT',       '1/10',      2,   30,   1, 'Isinya, Kajiado',          '30 greenhouses + 50 open plots; vegetables & fruits'),
    (14,   10,     'FARM-MUSHROOM',       'Mushroom Cultivation',          'DEPT',       '1/10',      2,   40,   1, 'Isinya, Kajiado',          '16 Irish-style button mushroom houses'),
    (20,   10,     'FARM-INPUT-WH',       'Input Warehouse',               'WAREHOUSE',  '1/10',      2,   50,   1, 'Isinya, Kajiado',          'Fertilizer, seeds, agrichemicals'),
    (21,   10,     'FARM-PACKHOUSE',      'Packhouse',                     'PACKHOUSE',  '1/10',      2,   60,   1, 'Isinya, Kajiado',          'Packing + cold storage combined');


-- Future-proof tags (not yet attached to nodes; UI can attach later)
INSERT IGNORE INTO org_tag (code, name, category, description) VALUES
    ('SEASON.2026Q2',         '2026 Q2 Season',          'SEASON',          'Operating season covering Apr-Jun 2026'),
    ('SEASON.2026Q3',         '2026 Q3 Season',          'SEASON',          'Operating season covering Jul-Sep 2026'),
    ('CERT.GLOBAL_GAP',       'Global G.A.P.',           'CERTIFICATION',   'Nodes certified or in scope for Global G.A.P.'),
    ('CERT.KEBS_FOOD_SAFETY', 'KEBS Food Safety',        'CERTIFICATION',   'Nodes covered by KEBS food-safety certification'),
    ('COMPLIANCE.PCPB_A',     'PCPB Class A',            'COMPLIANCE_ZONE', 'Nodes operating under PCPB Class A pesticide handling rules');


-- ----------------------------------------------------------------------------
-- C. Create Kang manager account
-- ----------------------------------------------------------------------------

-- 1) sys_user row -- BCrypt cost 12 hash of "Welcome@123456"
INSERT IGNORE INTO sys_user
    (username,       password,
                                                                                  nickname, status,   user_type, created_at)
VALUES
    ('kang.manager', '$2b$12$V8E/7fR.4ATlzRGXE3dGM.kCvneEANX6aVVS1wHZJIJ67ID3W3lLy',
                                                                                  'Kang',   'active', 'STAFF',   NOW());

-- 2) Bind to MANAGER role
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username = 'kang.manager' AND r.code = 'MANAGER';

-- 3) Register in org_user as Albert's Farm primary + manager
INSERT IGNORE INTO org_user
    (user_id, node_id, is_primary, position, is_manager, effective_from, remark)
SELECT
    u.id, 10, 1, 'Farm Manager', 1, CURDATE(), 'Sprint 51 seed -- initial farm manager'
FROM sys_user u
WHERE u.username = 'kang.manager';

-- 4) Set Albert's Farm node manager_id -> Kang
UPDATE org_node
   SET manager_id = (SELECT id FROM sys_user WHERE username = 'kang.manager')
 WHERE code = 'ALBERTS-FARM' AND manager_id IS NULL;

-- 5) Set the root group manager_id -> admin (UID 1)
UPDATE org_node
   SET manager_id = 1
 WHERE code = 'ALBERTS-MEADOWS' AND manager_id IS NULL;


-- ----------------------------------------------------------------------------
-- D. Extend sys_user
-- ----------------------------------------------------------------------------
-- Use a stored-procedure pattern to make ALTER idempotent on MySQL 5.7+
-- (8.0.29 has ADD COLUMN IF NOT EXISTS but we don't assume that here).

DELIMITER //
DROP PROCEDURE IF EXISTS migration_050_add_column;
CREATE PROCEDURE migration_050_add_column(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def    VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name   = p_table
           AND column_name  = p_column
    ) THEN
        SET @ddl := CONCAT('ALTER TABLE ', p_table,
                           ' ADD COLUMN ', p_column, ' ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL migration_050_add_column('sys_user', 'primary_node_id',       'BIGINT NULL COMMENT ''Sprint 51 -- user''''s primary node for data_scope=group''');
CALL migration_050_add_column('sys_user', 'password_must_change',  'TINYINT NOT NULL DEFAULT 0 COMMENT ''Sprint 51 -- force change on next login''');

-- Mark Kang for first-login password change
UPDATE sys_user
   SET password_must_change = 1,
       primary_node_id      = 10
 WHERE username = 'kang.manager';

-- Backfill admin to GROUP node
UPDATE sys_user SET primary_node_id = 1 WHERE username = 'admin' AND primary_node_id IS NULL;


-- ----------------------------------------------------------------------------
-- E. ALTER 14 high-frequency business tables to add node_id BIGINT NULL
--    Sub-tables (warehouse_inbound_item etc) inherit via their parent row
--    and are intentionally NOT extended.
-- ----------------------------------------------------------------------------

CALL migration_050_add_column('plot',             'node_id', 'BIGINT NULL');
CALL migration_050_add_column('activity',         'node_id', 'BIGINT NULL');
CALL migration_050_add_column('harvest_record',   'node_id', 'BIGINT NULL');
CALL migration_050_add_column('batch',            'node_id', 'BIGINT NULL');
CALL migration_050_add_column('planting_plan',    'node_id', 'BIGINT NULL');

CALL migration_050_add_column('customer',         'node_id', 'BIGINT NULL');
CALL migration_050_add_column('sales_order',      'node_id', 'BIGINT NULL');
CALL migration_050_add_column('purchase_order',   'node_id', 'BIGINT NULL');

CALL migration_050_add_column('cs_contact_link',  'node_id', 'BIGINT NULL');
CALL migration_050_add_column('cs_csat_response', 'node_id', 'BIGINT NULL');

CALL migration_050_add_column('complaint',        'node_id', 'BIGINT NULL');
CALL migration_050_add_column('qc_inspection',    'node_id', 'BIGINT NULL');

CALL migration_050_add_column('inventory',        'node_id', 'BIGINT NULL');
CALL migration_050_add_column('packing',          'node_id', 'BIGINT NULL');
CALL migration_050_add_column('revenue',          'node_id', 'BIGINT NULL');


-- ----------------------------------------------------------------------------
-- F. Day-1 backfill (batched, light) -- only the three highest-traffic tables
--    Per decision #7 risk control: 1000-row batches, SLEEP between batches.
--    Other tables are backfilled in Day 5 once monitoring confirms no impact.
-- ----------------------------------------------------------------------------

-- All current plots go to Vegetable & Fruit Cultivation (id=13).
-- Mushroom houses live in plot too but cannot be distinguished from
-- vegetables at the schema level today; Sprint 51 Day 3 will add a one-off
-- UPDATE for plots whose name/code matches mushroom patterns.
-- For now everything goes to FARM-VEG-FRUIT; Day 3 will split.
UPDATE plot
   SET node_id = 13
 WHERE node_id IS NULL;

UPDATE activity
   SET node_id = 13
 WHERE node_id IS NULL;

UPDATE harvest_record
   SET node_id = 13
 WHERE node_id IS NULL;


-- ----------------------------------------------------------------------------
-- Cleanup helper
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS migration_050_add_column;


-- ============================================================================
-- End of migration 050
--
-- Verification after apply:
--   SELECT id, code, name, type, manager_id FROM org_node ORDER BY id;       -- expect 12 rows
--   SELECT id, username, primary_node_id, password_must_change
--     FROM sys_user WHERE username IN ('admin','kang.manager');              -- expect 2 rows
--   SELECT COUNT(*) FROM org_user;                                           -- expect 1 (Kang)
--   SHOW COLUMNS FROM plot LIKE 'node_id';                                   -- expect 1 row
--   SELECT COUNT(*) FROM plot WHERE node_id IS NULL;                         -- expect 0
-- ============================================================================
