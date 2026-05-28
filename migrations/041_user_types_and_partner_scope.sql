-- ============================================================================
-- Sprint 37: User-type segmentation (STAFF / PARTNER / CUSTOMER) plus
-- per-user scope rows that limit a PARTNER to specific plots / customers /
-- time windows.
--
-- Three additions:
--   1. sys_user.user_type / org_name / linked_customer_id (1:1 customer link)
--   2. sys_user_partner_subtype - user can be both AGRONOMIST and GAP_AUDITOR
--   3. sys_user_scope - generic (scope_type, scope_id) rows + optional dates
-- ============================================================================

-- 1) sys_user columns ---------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN user_type VARCHAR(16) NOT NULL DEFAULT 'STAFF'
        COMMENT 'STAFF / PARTNER / CUSTOMER'
        AFTER status,
    ADD COLUMN org_name VARCHAR(128) NULL
        COMMENT 'External org name e.g. KEPHIS, Equity Bank, ABC Holdings'
        AFTER user_type,
    ADD COLUMN linked_customer_id BIGINT NULL
        COMMENT 'For user_type=CUSTOMER, the customer.id row this account represents'
        AFTER org_name;

CREATE INDEX idx_sys_user_type ON sys_user(user_type);
CREATE INDEX idx_sys_user_linked_customer ON sys_user(linked_customer_id);

-- 2) Partner subtype join (a user can have multiple subtypes) -----------------
CREATE TABLE sys_user_partner_subtype (
    user_id      BIGINT       NOT NULL,
    subtype_code VARCHAR(32)  NOT NULL
        COMMENT 'AGRONOMIST / GAP_AUDITOR / BANK_OFFICER / LANDLORD / INSURANCE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, subtype_code),
    KEY idx_subtype (subtype_code)
) ENGINE=InnoDB COMMENT='Partner user -> subtype (M:N)';

-- 3) Generic per-user scope ---------------------------------------------------
-- Examples:
--   ('PLOT',     17)      - landlord can see plot 17
--   ('CUSTOMER',  3)      - sales partner only sees customer 3
--   ('DATE_WINDOW', NULL) - row carries valid_from + valid_to instead of id
CREATE TABLE sys_user_scope (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    scope_type   VARCHAR(32)  NOT NULL
        COMMENT 'PLOT / CUSTOMER / WAREHOUSE / DATE_WINDOW / ALL',
    scope_id     BIGINT       NULL
        COMMENT 'NULL for ALL or DATE_WINDOW scopes',
    valid_from   DATE         NULL,
    valid_to     DATE         NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id),
    KEY idx_type_id (scope_type, scope_id)
) ENGINE=InnoDB COMMENT='Per-user data scope for PARTNER / CUSTOMER';
