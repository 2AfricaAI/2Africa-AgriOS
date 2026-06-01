-- ============================================================================
-- migration 045: consolidated business-table bootstrap
--
-- Why this exists
--   `schema.sql` is an early v3.0-era snapshot that only contains the
--   foundation tables (sys_user, sys_role, sys_menu, customer, plot, crop,
--   variety, packaging_spec, location_warehouse, packing, sales_order, ...).
--   The 30+ migrations 009 through 038 that came later add the bulk of the
--   business modules — procurement, inputs, stock, warehouse operations, QC,
--   complaints, recalls — but a clean `git clone + docker compose up` only
--   loads schema.sql and skips those.
--
--   Result: a fresh install boots, the admin can log in (after migration 044),
--   but clicking on Warehouse / QC / Procurement / Inputs / Complaints menus
--   throws SQLSyntaxErrorException: Table ... doesn't exist.
--
-- What this migration does
--   Idempotently re-declares every business table from migrations 010 / 011 /
--   015 / 018 / 019 / 023 / 029 / 030 / 031 / 032 / 033 / 034 / 035 / 036 /
--   038 using `CREATE TABLE IF NOT EXISTS`. Safe to re-run; safe on an
--   already-populated database; gives a fresh install a complete schema in
--   one shot.
--
--   ALTER statements from the original migrations (which mutate existing
--   tables) are NOT re-applied here — they remain in their numbered
--   migration files. Operators upgrading from an older v3.0 install should
--   still apply 009-044 in order to get those column additions.
--
-- See also
--   `backend/docker-compose.yml` — the MySQL service now mounts the entire
--   `migrations/` folder and an init script that auto-applies all of them
--   on first boot, so this file's safety net is rarely needed for new
--   deployments going forward.
-- ============================================================================


-- ---------- 010_create_revenue ---------------------------------------------
CREATE TABLE IF NOT EXISTS `revenue` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `order_id`         BIGINT        NOT NULL,
  `order_item_id`    BIGINT        NOT NULL,
  `fulfillment_id`   BIGINT        NOT NULL,
  `sku_id`           BIGINT        NOT NULL,
  `customer_id`      BIGINT        NOT NULL,
  `batch_id`         BIGINT        NULL,
  `qty`              DECIMAL(12,3) NOT NULL,
  `gross_amount`     DECIMAL(14,2) NOT NULL,
  `tax`              DECIMAL(14,2) NOT NULL DEFAULT 0,
  `net_amount`       DECIMAL(14,2) NOT NULL,
  `currency`         VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `recognition_date` DATE          NOT NULL,
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'recognized',
  `channel`          VARCHAR(32),
  `remark`           VARCHAR(255),
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`       BIGINT,
  KEY `idx_order` (`order_id`),
  KEY `idx_customer_date` (`customer_id`, `recognition_date`),
  KEY `idx_sku_date` (`sku_id`, `recognition_date`),
  KEY `idx_date` (`recognition_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Revenue log - V2.0 P&L fact table';


-- ---------- 011_action_item ------------------------------------------------
CREATE TABLE IF NOT EXISTS `action_item` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `rule_code`       VARCHAR(32)  NOT NULL,
  `severity`        VARCHAR(16)  NOT NULL DEFAULT 'medium',
  `category`        VARCHAR(32)  NOT NULL,
  `title`           VARCHAR(255) NOT NULL,
  `description`     VARCHAR(1000),
  `owner_role`      VARCHAR(32),
  `ref_type`        VARCHAR(32),
  `ref_id`          BIGINT,
  `ref_code`        VARCHAR(64),
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'open',
  `due_date`        DATE,
  `data_snapshot`   JSON,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `resolved_at`     DATETIME,
  `resolved_by`     BIGINT,
  `resolved_remark` VARCHAR(255),
  UNIQUE KEY `uk_rule_ref` (`rule_code`, `ref_type`, `ref_id`),
  KEY `idx_status_category` (`status`, `category`),
  KEY `idx_owner_role` (`owner_role`),
  KEY `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Action items - Sprint 10';


-- ---------- 015_payment_and_ar ---------------------------------------------
CREATE TABLE IF NOT EXISTS `payment` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32),
  `order_id`        BIGINT        NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `amount`          DECIMAL(14,2) NOT NULL,
  `currency`        VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) DEFAULT 1.0,
  `amount_kes`      DECIMAL(14,2) NOT NULL,
  `method`          VARCHAR(16)   NOT NULL,
  `payment_date`    DATE          NOT NULL,
  `reference_no`    VARCHAR(64),
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'cleared',
  `reconciled_by`   BIGINT,
  `reconciled_at`   DATETIME,
  `remark`          VARCHAR(255),
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT,
  `updated_at`      DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_order` (`order_id`),
  KEY `idx_customer_date` (`customer_id`, `payment_date`),
  KEY `idx_method` (`method`),
  KEY `idx_status` (`status`),
  KEY `idx_ref` (`reference_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment receipt log';


-- ---------- 018_collection_log_and_sms -------------------------------------
CREATE TABLE IF NOT EXISTS `collection_log` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `customer_id`      BIGINT        NOT NULL,
  `order_id`         BIGINT,
  `log_date`         DATE          NOT NULL,
  `channel`          VARCHAR(16)   NOT NULL,
  `contact_person`   VARCHAR(80),
  `outcome`          VARCHAR(32)   NOT NULL,
  `promised_date`    DATE,
  `promised_amount`  DECIMAL(14,2),
  `content`          TEXT,
  `next_action_date` DATE,
  `operator_id`      BIGINT        NOT NULL,
  `operator_name`    VARCHAR(80),
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`       DATETIME,
  KEY `idx_customer`       (`customer_id`, `log_date`),
  KEY `idx_order`          (`order_id`),
  KEY `idx_next_action`    (`next_action_date`),
  KEY `idx_promised_date`  (`promised_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Collection follow-up log - Sprint 16';

CREATE TABLE IF NOT EXISTS `sms_template` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`         VARCHAR(32)  NOT NULL UNIQUE,
  `name`         VARCHAR(80)  NOT NULL,
  `channel`      VARCHAR(16)  NOT NULL DEFAULT 'sms',
  `lang`         VARCHAR(8)   NOT NULL DEFAULT 'en',
  `content`      VARCHAR(500) NOT NULL,
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SMS / WhatsApp templates';

CREATE TABLE IF NOT EXISTS `sms_log` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `customer_id`     BIGINT       NOT NULL,
  `order_id`        BIGINT,
  `template_code`   VARCHAR(32),
  `channel`         VARCHAR(16)  NOT NULL DEFAULT 'sms',
  `phone`           VARCHAR(32)  NOT NULL,
  `content`         VARCHAR(500) NOT NULL,
  `provider`        VARCHAR(32),
  `provider_msg_id` VARCHAR(64),
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'sent',
  `error`           VARCHAR(255),
  `sent_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `operator_id`     BIGINT,
  KEY `idx_customer` (`customer_id`, `sent_at`),
  KEY `idx_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SMS / WhatsApp send log';


-- ---------- 019_procurement_supplier_po_ap ---------------------------------
CREATE TABLE IF NOT EXISTS `supplier` (
  `id`             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)  NOT NULL UNIQUE,
  `name`           VARCHAR(120) NOT NULL,
  `type`           VARCHAR(32)  NOT NULL,
  `tax_id`         VARCHAR(64),
  `contact_name`   VARCHAR(80),
  `contact_phone`  VARCHAR(32),
  `contact_email`  VARCHAR(120),
  `address`        VARCHAR(255),
  `credit_days`    INT          NOT NULL DEFAULT 0,
  `payment_terms`  VARCHAR(32),
  `since_date`     DATE,
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'active',
  `remark`         VARCHAR(500),
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME     ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`     DATETIME,
  KEY `idx_status`  (`status`),
  KEY `idx_type`    (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Supplier master data';

CREATE TABLE IF NOT EXISTS `purchase_order` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE,
  `supplier_id`     BIGINT        NOT NULL,
  `order_date`      DATE          NOT NULL,
  `expected_date`   DATE,
  `currency`        VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) NOT NULL DEFAULT 1.0,
  `total_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0,
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `payment_status`  VARCHAR(16)   NOT NULL DEFAULT 'unpaid',
  `paid_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0,
  `due_date`        DATE,
  `remark`          VARCHAR(500),
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT,
  `updated_at`      DATETIME      ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      DATETIME,
  KEY `idx_supplier`           (`supplier_id`),
  KEY `idx_status`             (`status`),
  KEY `idx_payment_status_due` (`payment_status`, `due_date`),
  KEY `idx_order_date`         (`order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order header';

CREATE TABLE IF NOT EXISTS `purchase_order_item` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `po_id`         BIGINT        NOT NULL,
  `input_type`    VARCHAR(32)   NOT NULL,
  `input_item_id` BIGINT,
  `description`   VARCHAR(255)  NOT NULL,
  `quantity`      DECIMAL(14,3) NOT NULL,
  `unit`          VARCHAR(16)   NOT NULL,
  `unit_price`    DECIMAL(14,2) NOT NULL,
  `amount`        DECIMAL(14,2) NOT NULL,
  `received_qty`  DECIMAL(14,3) NOT NULL DEFAULT 0,
  `remark`        VARCHAR(255),
  KEY `idx_po`           (`po_id`),
  KEY `idx_input_type`   (`input_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order line items';

CREATE TABLE IF NOT EXISTS `vendor_payment` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`          VARCHAR(32),
  `po_id`         BIGINT        NOT NULL,
  `supplier_id`   BIGINT        NOT NULL,
  `amount`        DECIMAL(14,2) NOT NULL,
  `currency`      VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`       DECIMAL(12,6) DEFAULT 1.0,
  `amount_kes`    DECIMAL(14,2) NOT NULL,
  `method`        VARCHAR(16)   NOT NULL,
  `payment_date`  DATE          NOT NULL,
  `reference_no`  VARCHAR(64),
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'cleared',
  `remark`        VARCHAR(255),
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`    BIGINT,
  KEY `idx_po`            (`po_id`),
  KEY `idx_supplier_date` (`supplier_id`, `payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vendor payment log';


-- ---------- 023_input_item -------------------------------------------------
CREATE TABLE IF NOT EXISTS `input_item` (
  `id`                   BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                 VARCHAR(32)  NOT NULL UNIQUE,
  `name`                 VARCHAR(128) NOT NULL,
  `name_en`              VARCHAR(128),
  `input_type`           VARCHAR(32)  NOT NULL,
  `spec`                 VARCHAR(128),
  `unit`                 VARCHAR(16)  NOT NULL DEFAULT 'kg',
  `active_ingredient`    VARCHAR(128),
  `registration_no`      VARCHAR(64),
  `phi_days`             INT          NOT NULL DEFAULT 0,
  `default_supplier_id`  BIGINT,
  `status`               VARCHAR(16)  NOT NULL DEFAULT 'active',
  `remark`               VARCHAR(255),
  `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `created_by`           BIGINT,
  `updated_at`           DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`           BIGINT,
  KEY `idx_input_type`        (`input_type`),
  KEY `idx_default_supplier`  (`default_supplier_id`),
  KEY `idx_status`            (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Input item master data';


-- ---------- 029_input_stock + 030_input_stock_log --------------------------
CREATE TABLE IF NOT EXISTS `input_stock` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `input_item_id`   BIGINT         NOT NULL,
  `warehouse_id`    BIGINT         NOT NULL,
  `qty_on_hand`     DECIMAL(14,3)  NOT NULL DEFAULT 0,
  `qty_reserved`    DECIMAL(14,3)  NOT NULL DEFAULT 0,
  `last_stock_at`   DATETIME       NULL,
  `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME       NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_item_warehouse` (`input_item_id`, `warehouse_id`),
  KEY `idx_warehouse`  (`warehouse_id`),
  KEY `idx_last_stock` (`last_stock_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Input item stock level per warehouse';

CREATE TABLE IF NOT EXISTS `input_stock_log` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `input_item_id`   BIGINT         NOT NULL,
  `warehouse_id`    BIGINT         NOT NULL,
  `direction`       ENUM('IN','OUT') NOT NULL,
  `qty`             DECIMAL(14,3)  NOT NULL,
  `reason_type`     VARCHAR(32)    NOT NULL,
  `reference_type`  VARCHAR(32)    NULL,
  `reference_id`    BIGINT         NULL,
  `qty_after`       DECIMAL(14,3)  NOT NULL,
  `operator_id`     BIGINT         NULL,
  `remark`          VARCHAR(255)   NULL,
  `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_log_item`      (`input_item_id`, `created_at` DESC),
  KEY `idx_log_warehouse` (`warehouse_id`, `created_at` DESC),
  KEY `idx_log_reason`    (`reason_type`),
  KEY `idx_log_ref`       (`reference_type`, `reference_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Input stock movement log';


-- ---------- 031_warehouse_inbound ------------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse_inbound` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE,
  `source_type`    VARCHAR(32)   NOT NULL,
  `source_id`      BIGINT        NULL,
  `warehouse_id`   BIGINT        NOT NULL,
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `confirmed_by`   BIGINT        NULL,
  `confirmed_at`   DATETIME      NULL,
  `remark`         VARCHAR(255)  NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`    BIGINT         NULL,
  `updated_at`     DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_inbound_status`  (`status`),
  KEY `idx_inbound_source`  (`source_type`, `source_id`),
  KEY `idx_inbound_wh`      (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse inbound order';

CREATE TABLE IF NOT EXISTS `warehouse_inbound_item` (
  `id`             BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `inbound_id`     BIGINT         NOT NULL,
  `input_item_id`  BIGINT         NOT NULL,
  `expected_qty`   DECIMAL(14,3)  NOT NULL,
  `actual_qty`     DECIMAL(14,3)  NULL,
  `remark`         VARCHAR(255)   NULL,
  KEY `idx_inbound_item`  (`inbound_id`),
  KEY `idx_inbound_ii`    (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse inbound order items';


-- ---------- 032_warehouse_outbound -----------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse_outbound` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE,
  `source_type`    VARCHAR(32)   NOT NULL,
  `source_id`      BIGINT        NULL,
  `warehouse_id`   BIGINT        NOT NULL,
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `picked_by`      BIGINT        NULL,
  `picked_at`      DATETIME      NULL,
  `confirmed_by`   BIGINT        NULL,
  `confirmed_at`   DATETIME      NULL,
  `remark`         VARCHAR(255)  NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT        NULL,
  `updated_at`     DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_outbound_status`  (`status`),
  KEY `idx_outbound_source`  (`source_type`, `source_id`),
  KEY `idx_outbound_wh`      (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse outbound order';

CREATE TABLE IF NOT EXISTS `warehouse_outbound_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `outbound_id`     BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `requested_qty`   DECIMAL(14,3)  NOT NULL,
  `picked_qty`      DECIMAL(14,3)  NULL,
  `actual_qty`      DECIMAL(14,3)  NULL,
  `remark`          VARCHAR(255)   NULL,
  KEY `idx_outbound_item`  (`outbound_id`),
  KEY `idx_outbound_ii`    (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse outbound order items';


-- ---------- 033_warehouse_stocktake ----------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse_stocktake` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE,
  `warehouse_id`   BIGINT        NOT NULL,
  `count_type`     VARCHAR(16)   NOT NULL DEFAULT 'full',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `counted_by`     BIGINT        NULL,
  `counted_at`     DATETIME      NULL,
  `confirmed_by`   BIGINT        NULL,
  `confirmed_at`   DATETIME      NULL,
  `remark`         VARCHAR(255)  NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT        NULL,
  `updated_at`     DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_st_status`    (`status`),
  KEY `idx_st_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse stocktake / inventory count';

CREATE TABLE IF NOT EXISTS `warehouse_stocktake_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `stocktake_id`    BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `system_qty`      DECIMAL(14,3)  NOT NULL,
  `count_qty`       DECIMAL(14,3)  NULL,
  `diff_qty`        DECIMAL(14,3)  NULL,
  `remark`          VARCHAR(255)   NULL,
  KEY `idx_sti_parent`  (`stocktake_id`),
  KEY `idx_sti_item`    (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stocktake line items';


-- ---------- 034_warehouse_transfer -----------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse_transfer` (
  `id`                BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`              VARCHAR(32)   NOT NULL UNIQUE,
  `from_warehouse_id` BIGINT        NOT NULL,
  `to_warehouse_id`   BIGINT        NOT NULL,
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `confirmed_by`      BIGINT        NULL,
  `confirmed_at`      DATETIME      NULL,
  `remark`            VARCHAR(255)  NULL,
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`        BIGINT        NULL,
  `updated_at`        DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_tr_status` (`status`),
  KEY `idx_tr_from`   (`from_warehouse_id`),
  KEY `idx_tr_to`     (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse transfer order';

CREATE TABLE IF NOT EXISTS `warehouse_transfer_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `transfer_id`     BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `qty`             DECIMAL(14,3)  NOT NULL,
  `remark`          VARCHAR(255)   NULL,
  KEY `idx_tri_parent` (`transfer_id`),
  KEY `idx_tri_item`   (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transfer order items';


-- ---------- 035_warehouse_scrap --------------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse_scrap` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE,
  `warehouse_id`   BIGINT        NOT NULL,
  `scrap_type`     VARCHAR(16)   NOT NULL DEFAULT 'damaged',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft',
  `confirmed_by`   BIGINT        NULL,
  `confirmed_at`   DATETIME      NULL,
  `remark`         VARCHAR(255)  NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT        NULL,
  `updated_at`     DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_sc_status`    (`status`),
  KEY `idx_sc_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Warehouse scrap / write-off order';

CREATE TABLE IF NOT EXISTS `warehouse_scrap_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `scrap_id`        BIGINT         NOT NULL,
  `input_item_id`   BIGINT         NOT NULL,
  `qty`             DECIMAL(14,3)  NOT NULL,
  `reason`          VARCHAR(255)   NULL,
  KEY `idx_sci_parent` (`scrap_id`),
  KEY `idx_sci_item`   (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Scrap order items';


-- ---------- 036_qc_inspection ----------------------------------------------
CREATE TABLE IF NOT EXISTS `qc_inspection` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE,
  `inspection_type` VARCHAR(16)   NOT NULL,
  `ref_type`        VARCHAR(32)   NULL,
  `ref_id`          BIGINT        NULL,
  `ref_code`        VARCHAR(64)   NULL,
  `inspect_date`    DATE          NOT NULL,
  `inspector_id`    BIGINT        NULL,
  `result`          VARCHAR(20)   NOT NULL DEFAULT 'pending',
  `result_remark`   VARCHAR(500)  NULL,
  `photo_ids`       JSON          NULL,
  `remark`          VARCHAR(500)  NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT         NULL,
  `updated_at`     DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_qc_type`     (`inspection_type`),
  KEY `idx_qc_result`   (`result`),
  KEY `idx_qc_ref`      (`ref_type`, `ref_id`),
  KEY `idx_qc_date`     (`inspect_date` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QC inspection orders';

CREATE TABLE IF NOT EXISTS `qc_inspection_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `inspection_id`   BIGINT         NOT NULL,
  `check_point`     VARCHAR(64)    NOT NULL,
  `expected_value`  VARCHAR(128)   NULL,
  `actual_value`    VARCHAR(128)   NULL,
  `result`          VARCHAR(8)     NULL DEFAULT 'pending',
  `remark`          VARCHAR(255)   NULL,
  KEY `idx_qci_parent` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QC inspection items';


-- ---------- 038_complaint_and_recall ---------------------------------------
CREATE TABLE IF NOT EXISTS `complaint` (
  `id`                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `code`                VARCHAR(32)    NOT NULL UNIQUE,
  `reported_at`         DATETIME       NOT NULL,
  `customer_id`         BIGINT         NULL,
  `order_id`            BIGINT         NULL,
  `batch_id`            BIGINT         NULL,
  `sku_id`              BIGINT         NULL,
  `category`            VARCHAR(32)    NOT NULL,
  `severity`            VARCHAR(16)    NOT NULL DEFAULT 'medium',
  `channel`             VARCHAR(16)    NOT NULL DEFAULT 'phone',
  `description`         TEXT           NOT NULL,
  `photo_ids`           JSON           NULL,
  `status`              VARCHAR(24)    NOT NULL DEFAULT 'open',
  `resolution`          TEXT           NULL,
  `resolution_amount`   DECIMAL(12,2)  NULL,
  `reported_by_id`      BIGINT         NULL,
  `resolved_at`         DATETIME       NULL,
  `resolved_by_id`      BIGINT         NULL,
  `recall_id`           BIGINT         NULL,
  `created_at`          DATETIME       DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_customer`    (`customer_id`),
  KEY `idx_order`       (`order_id`),
  KEY `idx_batch`       (`batch_id`),
  KEY `idx_status`      (`status`),
  KEY `idx_reported_at` (`reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer / QC complaints';

CREATE TABLE IF NOT EXISTS `recall` (
  `id`                       BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                     VARCHAR(32)  NOT NULL UNIQUE,
  `triggered_at`             DATETIME     NOT NULL,
  `source_complaint_id`      BIGINT       NULL,
  `batch_id`                 BIGINT       NOT NULL,
  `scope`                    VARCHAR(24)  NOT NULL DEFAULT 'batch_only',
  `reason`                   TEXT         NOT NULL,
  `status`                   VARCHAR(24)  NOT NULL DEFAULT 'initiated',
  `affected_order_count`     INT          NOT NULL DEFAULT 0,
  `affected_customer_count`  INT          NOT NULL DEFAULT 0,
  `affected_qty`             DECIMAL(14,3) NOT NULL DEFAULT 0,
  `initiated_by_id`          BIGINT       NULL,
  `closed_at`                DATETIME     NULL,
  `closed_by_id`             BIGINT       NULL,
  `closed_remark`            VARCHAR(500) NULL,
  `created_at`               DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`               DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_batch`        (`batch_id`),
  KEY `idx_status`       (`status`),
  KEY `idx_triggered_at` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Batch recall';

CREATE TABLE IF NOT EXISTS `recall_affected_order` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `recall_id`       BIGINT        NOT NULL,
  `order_id`        BIGINT        NOT NULL,
  `order_code`      VARCHAR(64)   NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `customer_name`   VARCHAR(128)  NOT NULL,
  `qty`             DECIMAL(14,3) NOT NULL,
  `unit`            VARCHAR(8)    DEFAULT 'pack',
  `delivered_at`    DATETIME      NULL,
  `notified_at`     DATETIME      NULL,
  `notified_by_id`  BIGINT        NULL,
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_recall`   (`recall_id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recall affected orders snapshot';
