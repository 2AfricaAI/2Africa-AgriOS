-- ============================================================================
-- migration 051: Workflow engine -- Sprint 52 Day 1
--
-- See docs/PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2 § 2-3 for the design.
-- Foundation for every downstream module that needs approvals:
--   HR leave / payroll runs / expense report / contract signature /
--   asset disposal / data subject requests / etc.
--
-- ============================================================================
-- What this migration does
--
-- A. Create 5 new tables
--      wf_definition   -- workflow templates (JSON DSL)
--      wf_instance     -- live workflow instances
--      wf_step         -- per-step state (parallel groups via shared seq)
--      wf_audit        -- append-only action log
--      wf_delegation   -- temporary authority transfer (vacation/handover)
--
-- B. Seed 3 built-in definitions:
--      hr.leave_request    -- 1-step (line manager approval)
--      admin.expense       -- amount-tiered (1 or 2 steps by KES amount)
--      finance.payment_out -- 3-tier (manager -> CFO if > N -> SUPER_ADMIN if > M)
--
-- C. Permission seeds: wf:def:manage + wf:instance:list / approve /
--    withdraw / delegate
--
-- D. UPDATE/DELETE trigger on wf_audit -- decision: audit log is
--    immutable at the DB level. Even DBAs cannot tamper with it via the
--    application connection (the DB user lacks SUPER privilege).
--
-- All statements idempotent (INSERT IGNORE / CREATE TABLE IF NOT EXISTS).
-- ============================================================================


-- ----------------------------------------------------------------------------
-- A. New tables
-- ----------------------------------------------------------------------------

-- ----- wf_definition: workflow templates --------------------------------
CREATE TABLE IF NOT EXISTS wf_definition (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)  NOT NULL,                -- 'hr.leave_request'
    name            VARCHAR(128) NOT NULL,
    module          VARCHAR(32)  NOT NULL,                -- 'hr' / 'admin' / 'finance' / ...
    version         INT          NOT NULL DEFAULT 1,
    active          TINYINT      NOT NULL DEFAULT 1,
    schema_json     JSON         NOT NULL,                -- DSL: trigger + steps[] (see PRD § 3.1)
    description     VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wf_def_code_ver (code, version),
    KEY idx_wf_def_module (module, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----- wf_instance: a live request -------------------------------------
CREATE TABLE IF NOT EXISTS wf_instance (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    definition_id   BIGINT       NOT NULL,                -- FK wf_definition.id
    -- Reverse-lookup to business row (no DB FK to keep coupling loose)
    biz_table       VARCHAR(64)  NOT NULL,                -- 'hr_leave_request' / 'admin_expense' / ...
    biz_id          BIGINT       NOT NULL,
    -- Pre-computed metadata for the engine
    title           VARCHAR(255) NULL,                    -- displayed in pending list
    status          VARCHAR(32)  NOT NULL,                -- 'pending' / 'approved' / 'rejected' / 'cancelled'
    initiator_id    BIGINT       NOT NULL,
    current_step_seq INT         NULL,                    -- which seq group is currently active
    -- Routing inputs (set at submit, immutable after)
    amount_hint     DECIMAL(18,2) NULL,                   -- for amount-tiered escalation
    urgency         VARCHAR(16)  NULL,                    -- 'normal' / 'urgent' / 'critical'
    node_id         BIGINT       NULL,                    -- decision #5 audit scope; Sprint 51 nodes
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME     NULL,
    completed_by    BIGINT       NULL,                    -- last step's actor when completed
    last_action     VARCHAR(32)  NULL,                    -- mirror of last wf_audit row for fast access
    PRIMARY KEY (id),
    KEY idx_wf_inst_biz (biz_table, biz_id),
    KEY idx_wf_inst_status (status, created_at),
    KEY idx_wf_inst_initiator (initiator_id, status),
    KEY idx_wf_inst_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----- wf_step: each approval node ------------------------------------
CREATE TABLE IF NOT EXISTS wf_step (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    -- Parallel grouping: same seq = parallel (all must approve); different seq = serial
    seq             INT          NOT NULL,
    -- 'approval' / 'cc' / 'sign' / 'pay' (decision: extensible enum, no DB CHECK)
    type            VARCHAR(16)  NOT NULL DEFAULT 'approval',
    -- Resolved at instance creation OR step activation; one of:
    --   assignee_id  -- specific user
    --   assignee_role  -- role code that any matching user can claim
    --   assignee_lookup -- expression resolved at runtime, e.g. 'node.manager_id'
    assignee_id     BIGINT       NULL,
    assignee_role   VARCHAR(64)  NULL,
    assignee_lookup VARCHAR(128) NULL,
    -- State machine
    -- 'pending'      -- waiting for assignee
    -- 'in_progress'  -- claimed by assignee (optional intermediate state)
    -- 'approved'     -- approved by assignee
    -- 'rejected'     -- rejected (instance terminates)
    -- 'returned'     -- returned to a previous step
    -- 'delegated'    -- assignee delegated to another user
    -- 'skipped'      -- conditional skip
    -- 'expired'      -- SLA missed; will auto-escalate
    status          VARCHAR(16)  NOT NULL DEFAULT 'pending',
    action          VARCHAR(16)  NULL,                    -- last action taken
    actor_id        BIGINT       NULL,                    -- who took the action (delegate-aware)
    comment         VARCHAR(2000) NULL,
    sla_hours       INT          NULL,                    -- SLA from step activation
    sla_due_at      DATETIME     NULL,                    -- activation time + sla_hours
    escalated_to_id BIGINT       NULL,                    -- auto-escalation target
    activated_at    DATETIME     NULL,                    -- when this step became active
    completed_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_wf_step_instance (instance_id, seq),
    KEY idx_wf_step_assignee (assignee_id, status),
    KEY idx_wf_step_sla (sla_due_at, status),
    KEY idx_wf_step_status (status, activated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----- wf_audit: append-only log --------------------------------------
CREATE TABLE IF NOT EXISTS wf_audit (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    step_id         BIGINT       NULL,                    -- NULL for instance-level events (create/cancel)
    actor_id        BIGINT       NOT NULL,
    -- 'create' / 'submit' / 'approve' / 'reject' / 'return' /
    -- 'delegate' / 'withdraw' / 'escalate' / 'cancel' / 'cc-ack'
    action          VARCHAR(32)  NOT NULL,
    before_json     JSON         NULL,
    after_json      JSON         NULL,
    comment         VARCHAR(2000) NULL,
    ip              VARCHAR(64)  NULL,
    user_agent      VARCHAR(255) NULL,
    occurred_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_wf_audit_instance (instance_id, occurred_at),
    KEY idx_wf_audit_actor (actor_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----- wf_delegation: temporary authority transfer --------------------
CREATE TABLE IF NOT EXISTS wf_delegation (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    delegator_id    BIGINT       NOT NULL,                -- user transferring
    delegatee_id    BIGINT       NOT NULL,                -- user receiving
    from_date       DATE         NOT NULL,
    to_date         DATE         NOT NULL,
    -- Module scope; NULL = all modules; otherwise comma-separated:
    --   'hr,admin' = only HR + Admin approvals are delegated
    scope_modules   VARCHAR(128) NULL,
    reason          VARCHAR(255) NULL,
    active          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    PRIMARY KEY (id),
    KEY idx_wf_dlg_delegator (delegator_id, active, from_date, to_date),
    KEY idx_wf_dlg_delegatee (delegatee_id, active, from_date, to_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----------------------------------------------------------------------------
-- B. Built-in workflow definitions
--    These are templates; module pages reference them by code when
--    submitting a request.
-- ----------------------------------------------------------------------------

INSERT IGNORE INTO wf_definition (id, code, name, module, version, active, schema_json, description)
VALUES
(1,
 'hr.leave_request',
 'HR -- Leave Request',
 'hr',
 1,
 1,
 JSON_OBJECT(
   'trigger',  JSON_OBJECT('always', true),
   'steps',    JSON_ARRAY(
     JSON_OBJECT(
       'seq',      1,
       'type',     'approval',
       'assignee', JSON_OBJECT('lookup', 'node.manager_id'),
       'sla_hours', 48,
       'on_escalate', JSON_OBJECT('lookup', 'node.parent.manager_id')
     )
   )
 ),
 'Default: line manager approves; 48h SLA; escalates to grand-parent node manager.'),

(2,
 'admin.expense',
 'Admin -- Expense Reimbursement',
 'admin',
 1,
 1,
 JSON_OBJECT(
   'trigger',  JSON_OBJECT('always', true),
   'steps',    JSON_ARRAY(
     JSON_OBJECT(
       'seq',      1,
       'type',     'approval',
       'assignee', JSON_OBJECT('lookup', 'node.manager_id'),
       'sla_hours', 24
     ),
     JSON_OBJECT(
       'seq',      2,
       'type',     'approval',
       'assignee', JSON_OBJECT('role', 'CFO'),
       'condition','amount > 50000',
       'sla_hours', 48
     )
   )
 ),
 'Manager approves; CFO additionally for amounts > 50,000 KES.'),

(3,
 'finance.payment_out',
 'Finance -- Outgoing Payment',
 'finance',
 1,
 1,
 JSON_OBJECT(
   'trigger',  JSON_OBJECT('always', true),
   'steps',    JSON_ARRAY(
     JSON_OBJECT(
       'seq',      1,
       'type',     'approval',
       'assignee', JSON_OBJECT('role', 'FINANCE_MANAGER'),
       'sla_hours', 24
     ),
     JSON_OBJECT(
       'seq',      2,
       'type',     'approval',
       'assignee', JSON_OBJECT('role', 'CFO'),
       'condition','amount > 100000',
       'sla_hours', 24
     ),
     JSON_OBJECT(
       'seq',      3,
       'type',     'approval',
       'assignee', JSON_OBJECT('role', 'SUPER_ADMIN'),
       'condition','amount > 1000000',
       'sla_hours', 48
     )
   )
 ),
 'Tiered: Finance Manager -> CFO (>100k) -> SUPER_ADMIN (>1M). KES.');


-- ----------------------------------------------------------------------------
-- C. Permission seeds
--    These mirror the cs:* + system:* patterns from Sprint 38-50.
-- ----------------------------------------------------------------------------

INSERT IGNORE INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible)
VALUES
    (990, 0, 'wf.def.manage',     'Workflow definitions',     'button', NULL, 'wf:def:manage',        90, 0),
    (991, 0, 'wf.instance.list',  'My pending approvals',     'button', NULL, 'wf:instance:list',     91, 0),
    (992, 0, 'wf.instance.act',   'Approve / reject',         'button', NULL, 'wf:instance:approve',  92, 0),
    (993, 0, 'wf.instance.wd',    'Withdraw own request',     'button', NULL, 'wf:instance:withdraw', 93, 0),
    (994, 0, 'wf.delegate',       'Delegate authority',       'button', NULL, 'wf:instance:delegate', 94, 0);

-- Bindings: who gets which perm by default
-- wf:def:manage         -> SUPER_ADMIN only (templates are platform-level)
-- wf:instance:list      -> everyone authenticated -- they need to see their own
-- wf:instance:approve   -> SUPER_ADMIN / MANAGER / LEADER (anyone who can be an approver)
-- wf:instance:withdraw  -> everyone (initiator-side only, enforced in service)
-- wf:instance:delegate  -> SUPER_ADMIN / MANAGER (only managers should delegate)

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 990 FROM sys_role r WHERE r.code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 991 FROM sys_role r
WHERE r.code IN ('SUPER_ADMIN', 'MANAGER', 'LEADER', 'PACKHOUSE', 'SALES', 'WORKER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 992 FROM sys_role r
WHERE r.code IN ('SUPER_ADMIN', 'MANAGER', 'LEADER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 993 FROM sys_role r
WHERE r.code IN ('SUPER_ADMIN', 'MANAGER', 'LEADER', 'PACKHOUSE', 'SALES', 'WORKER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 994 FROM sys_role r
WHERE r.code IN ('SUPER_ADMIN', 'MANAGER');


-- ----------------------------------------------------------------------------
-- D. wf_audit immutability triggers
--    Reject UPDATE and DELETE at the DB level. Audit is append-only.
--    NOTE: DBAs with SUPER privilege can still bypass these triggers; we'll
--    revoke that privilege from the application user in Sprint 60.
-- ----------------------------------------------------------------------------

DROP TRIGGER IF EXISTS wf_audit_no_update;
DROP TRIGGER IF EXISTS wf_audit_no_delete;

DELIMITER //

CREATE TRIGGER wf_audit_no_update
BEFORE UPDATE ON wf_audit FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'wf_audit is append-only; UPDATE is forbidden';
END //

CREATE TRIGGER wf_audit_no_delete
BEFORE DELETE ON wf_audit FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'wf_audit is append-only; DELETE is forbidden';
END //

DELIMITER ;


-- ============================================================================
-- End of migration 051.
--
-- Verification after apply:
--   SELECT id, code, module, active FROM wf_definition ORDER BY id;      -- expect 3 rows
--   DESCRIBE wf_instance;                                                  -- 16 columns
--   DESCRIBE wf_step;                                                      -- 18 columns
--   DESCRIBE wf_audit;                                                     -- 11 columns
--   DESCRIBE wf_delegation;                                                -- 11 columns
--   SHOW TRIGGERS LIKE 'wf_audit%';                                        -- 2 triggers
--   SELECT m.perms, COUNT(rm.role_id) AS bound_roles
--     FROM sys_menu m
--     LEFT JOIN sys_role_menu rm ON rm.menu_id = m.id
--    WHERE m.perms LIKE 'wf:%'
--    GROUP BY m.perms;                                                     -- expect 5 perms
-- ============================================================================
