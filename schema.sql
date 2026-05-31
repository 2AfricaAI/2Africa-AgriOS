-- ============================================================================
-- 2Africa AgriOS - Phase 1 MVP Database Schema
-- MySQL 8.0+ / InnoDB / utf8mb4
-- Version: V1.0
-- Date: 2026-05-19
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS `toafrica_agrios`;
CREATE DATABASE `toafrica_agrios`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `toafrica_agrios`;

-- ============================================================================
-- 1. SYSTEM / RBAC
-- ============================================================================

CREATE TABLE `sys_user` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `username`      VARCHAR(64)  NOT NULL UNIQUE,
  `password`      VARCHAR(128) NOT NULL COMMENT 'BCrypt',
  `nickname`      VARCHAR(64),
  `phone`         VARCHAR(20),
  `email`         VARCHAR(128),
  `avatar`        VARCHAR(255),
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/locked/disabled',
  `last_login_at` DATETIME,
  `last_login_ip` VARCHAR(64),
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`    BIGINT,
  `updated_at`    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     NULL,
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='System user';

CREATE TABLE `sys_role` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`       VARCHAR(64)  NOT NULL UNIQUE COMMENT 'WORKER/LEADER/PACKHOUSE/SALES/MANAGER',
  `name`       VARCHAR(64)  NOT NULL,
  `data_scope` VARCHAR(16)  NOT NULL DEFAULT 'self' COMMENT 'self/group/all',
  `remark`     VARCHAR(255),
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Role';

CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB COMMENT='User-Role';

CREATE TABLE `sys_menu` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `parent_id`  BIGINT       NOT NULL DEFAULT 0,
  `code`       VARCHAR(64)  NOT NULL UNIQUE,
  `name`       VARCHAR(64)  NOT NULL,
  `type`       VARCHAR(16)  NOT NULL COMMENT 'menu/button',
  `path`       VARCHAR(255),
  `component`  VARCHAR(255),
  `perms`      VARCHAR(255) COMMENT 'Permission key, e.g. plot:list',
  `icon`       VARCHAR(64),
  `sort`       INT          NOT NULL DEFAULT 0,
  `visible`    TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Menus / buttons';

CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB COMMENT='Role-Menu';

CREATE TABLE `sys_dict_type` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `dict_type`  VARCHAR(64)  NOT NULL UNIQUE,
  `dict_name`  VARCHAR(128) NOT NULL,
  `remark`     VARCHAR(255),
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Dictionary type';

CREATE TABLE `sys_dict_data` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `dict_type`  VARCHAR(64)  NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(128) NOT NULL,
  `sort`       INT          NOT NULL DEFAULT 0,
  `css_class`  VARCHAR(64),
  `status`     TINYINT(1)   NOT NULL DEFAULT 1,
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB COMMENT='Dictionary data';

CREATE TABLE `sys_oper_log` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT,
  `username`     VARCHAR(64),
  `module`       VARCHAR(64),
  `action`       VARCHAR(64),
  `target_type`  VARCHAR(64),
  `target_id`    BIGINT,
  `method`       VARCHAR(8),
  `url`          VARCHAR(255),
  `req_params`   TEXT,
  `resp_status`  INT,
  `ip`           VARCHAR(64),
  `cost_ms`      INT,
  `error_msg`    VARCHAR(500),
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_time` (`user_id`,`created_at`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB COMMENT='Operation log';

CREATE TABLE `sys_file` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `object_key`    VARCHAR(512) NOT NULL UNIQUE COMMENT 'MinIO/OSS object key',
  `original_name` VARCHAR(255) NOT NULL COMMENT 'Original filename at upload time',
  `bucket`        VARCHAR(64)  NOT NULL,
  `size_bytes`    BIGINT       NOT NULL,
  `mime_type`     VARCHAR(128),
  `ext`           VARCHAR(16),
  `biz_type`      VARCHAR(32)  COMMENT 'Business category: avatar/crop_image/activity_photo/...',
  `uploaded_by`   BIGINT,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     NULL,
  KEY `idx_uploaded_by` (`uploaded_by`),
  KEY `idx_biz_type`    (`biz_type`),
  KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB COMMENT='File upload record';

CREATE TABLE `sys_code_rule` (
  `id`         BIGINT      PRIMARY KEY AUTO_INCREMENT,
  `rule_key`   VARCHAR(64) NOT NULL UNIQUE,
  `template`   VARCHAR(128) NOT NULL COMMENT 'e.g. B-{yyyyMMdd}-{plotCode}-{seq:02d}',
  `remark`     VARCHAR(255),
  `created_at` DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Code rule';

-- ============================================================================
-- 2. MASTER DATA
-- ============================================================================

CREATE TABLE `crop` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL UNIQUE,
  `name`            VARCHAR(64)  NOT NULL,
  `category`        VARCHAR(32)  COMMENT 'leafy / fruit / root ...',
  `unit`            VARCHAR(8)   NOT NULL DEFAULT 'kg',
  `cycle_days`      INT          COMMENT 'Standard growth cycle',
  `shelf_life_days` INT          NULL DEFAULT NULL COMMENT 'Default shelf life (days) after packing — FEFO',
  `remark`          VARCHAR(255),
  `status`          TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Crop';

CREATE TABLE `variety` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `crop_id`         BIGINT       NOT NULL,
  `code`            VARCHAR(32)  NOT NULL,
  `name`            VARCHAR(64)  NOT NULL,
  `traits`          VARCHAR(255) COMMENT 'Traits',
  `shelf_life_days` INT          NULL DEFAULT NULL COMMENT 'Override of crop.shelf_life_days; NULL = use crop default',
  `status`          TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_crop_code` (`crop_id`,`code`),
  KEY `idx_crop` (`crop_id`)
) ENGINE=InnoDB COMMENT='Variety';

CREATE TABLE `input` (
  `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL UNIQUE,
  `name`        VARCHAR(128) NOT NULL,
  `type`        VARCHAR(16)  NOT NULL COMMENT 'fertilizer/pesticide/seed/film/other',
  `unit`        VARCHAR(8)   NOT NULL,
  `supplier`    VARCHAR(128),
  `safety_days` INT          DEFAULT 0 COMMENT 'Safety interval (days)',
  `stock_qty`   DECIMAL(12,3) NOT NULL DEFAULT 0,
  `status`      TINYINT(1)   NOT NULL DEFAULT 1,
  `remark`      VARCHAR(255),
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_type` (`type`)
) ENGINE=InnoDB COMMENT='Input';

CREATE TABLE `packaging_spec` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL UNIQUE,
  `name`            VARCHAR(64)  NOT NULL COMMENT 'e.g. 250g clear box',
  `unit_net_kg`     DECIMAL(8,3) NOT NULL COMMENT 'Per-unit net weight (kg)',
  `unit_gross_kg`   DECIMAL(8,3) COMMENT 'Per-unit gross weight (kg)',
  `material`        VARCHAR(64),
  `status`          TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Packaging spec';

CREATE TABLE `location_warehouse` (
  `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL UNIQUE COMMENT 'e.g. W01-A1, IW-SEED',
  `name`        VARCHAR(64)  NOT NULL,
  `type`        VARCHAR(16)  NOT NULL DEFAULT 'normal' COMMENT 'PHYSICAL: normal/cold/quarantine',
  `purpose`     VARCHAR(32)  NOT NULL DEFAULT 'finished_goods'
                COMMENT 'BUSINESS (Sprint 22): finished_goods | seed_storage | fertilizer_storage | pesticide_storage | construction_storage | spare_parts_storage | tools_storage | packaging_storage | other_storage',
  `level`       VARCHAR(16)  NOT NULL DEFAULT 'warehouse'
                COMMENT 'HIERARCHY (Sprint 22.0.5): warehouse | zone | shelf | bin',
  `parent_id`   BIGINT       DEFAULT 0,
  `capacity_kg` DECIMAL(12,2),
  `status`      TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_warehouse_purpose` (`purpose`),
  KEY `idx_warehouse_level` (`level`)
) ENGINE=InnoDB COMMENT='Warehouses / locations';

-- ============================================================================
-- 3. PEOPLE
-- ============================================================================

CREATE TABLE `staff` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT       UNIQUE COMMENT 'Linked system account',
  `code`         VARCHAR(32)  NOT NULL UNIQUE,
  `name`         VARCHAR(64)  NOT NULL,
  `phone`        VARCHAR(20),
  `id_card`      VARCHAR(32),
  `role_type`    VARCHAR(32)  NOT NULL COMMENT 'worker/leader/packhouse/sales/manager',
  `team_code`    VARCHAR(32)  COMMENT 'Team',
  `hire_date`    DATE,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'active',
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`   DATETIME     NULL,
  KEY `idx_role` (`role_type`),
  KEY `idx_team` (`team_code`)
) ENGINE=InnoDB COMMENT='Staff';

-- ============================================================================
-- 4. PRODUCTION
-- ============================================================================

CREATE TABLE `plot` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE COMMENT 'P-001',
  `name`           VARCHAR(64)   NOT NULL,
  `area_mu`        DECIMAL(10,2) NOT NULL CHECK (`area_mu` > 0),
  `location`       VARCHAR(128)  COMMENT 'Lat/lng or description',
  `soil_type`      VARCHAR(32),
  `irrigation`     VARCHAR(32),
  `owner_id`       BIGINT        NOT NULL COMMENT 'staff_id',
  `allowed_crops`  JSON          COMMENT 'Allowed crop id array',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT 'active/inactive/fallow',
  `remark`         VARCHAR(255),
  `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`     BIGINT,
  `deleted_at`     DATETIME      NULL,
  KEY `idx_owner_status` (`owner_id`,`status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Plot';

CREATE TABLE `planting_plan` (
  `id`                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`               VARCHAR(32)   NOT NULL UNIQUE COMMENT 'PL-26-0001',
  `plot_id`            BIGINT        NOT NULL,
  `crop_id`            BIGINT        NOT NULL,
  `variety_id`         BIGINT,
  `area_mu`            DECIMAL(10,2) NOT NULL,
  `plan_start_date`    DATE          NOT NULL,
  `plan_harvest_date`  DATE          NOT NULL,
  `actual_start_date`  DATE,
  `actual_finish_date` DATE,
  `target_yield_kg`    DECIMAL(12,2),
  `status`             VARCHAR(16)   NOT NULL DEFAULT 'draft'
                       COMMENT 'draft/planned/in_progress/harvested/completed/cancelled',
  `remark`             VARCHAR(255),
  `created_at`         DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`         BIGINT,
  `updated_at`         DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`         BIGINT,
  `deleted_at`         DATETIME      NULL,
  KEY `idx_plot_status` (`plot_id`,`status`),
  KEY `idx_dates` (`plan_start_date`,`plan_harvest_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Planting plan';

CREATE TABLE `activity` (
  `id`               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `client_uuid`      VARCHAR(64)  UNIQUE COMMENT 'Idempotency key',
  `plot_id`          BIGINT       NOT NULL,
  `plan_id`          BIGINT       NOT NULL,
  `activity_type`    VARCHAR(16)  NOT NULL COMMENT 'sow/fertilize/spray/weed/water/prune/other',
  `occur_date`       DATE         NOT NULL,
  `operator_id`     BIGINT       NOT NULL COMMENT 'staff_id',
  `photos`          JSON         COMMENT 'OSS URL array',
  `location_gps`    VARCHAR(64),
  `remark`          VARCHAR(500),
  `labor_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Labor cost (Sprint 11)',
  `water_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Water cost',
  `electricity_cost` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Electricity cost',
  `fertilizer_cost`  DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Fertilizer cost',
  `other_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Other cost',
  `cost_currency`    VARCHAR(8)    NOT NULL DEFAULT 'KES' COMMENT 'Shared currency for all cost fields on this activity',
  `audit_status`     VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `auditor_id`     BIGINT,
  `audited_at`     DATETIME,
  `audit_remark`   VARCHAR(255),
  `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_plot_date` (`plot_id`,`occur_date`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB COMMENT='Field activity record';

CREATE TABLE `activity_input` (
  `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `activity_id` BIGINT       NOT NULL,
  `input_id`    BIGINT       NOT NULL,
  `qty`         DECIMAL(12,3) NOT NULL,
  `unit`        VARCHAR(8)   NOT NULL,
  `cost`        DECIMAL(12,2),
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_activity` (`activity_id`),
  KEY `idx_input` (`input_id`)
) ENGINE=InnoDB COMMENT='Activity-input lines';

-- Sprint 21.1 (Phase 4) - 投入品主数据
CREATE TABLE `input_item` (
  `id`                   BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                 VARCHAR(32)  NOT NULL UNIQUE COMMENT 'II-0001',
  `name`                 VARCHAR(128) NOT NULL,
  `name_en`              VARCHAR(128),
  `input_type`           VARCHAR(32)  NOT NULL COMMENT 'fertilizer/pesticide/seed/film/labor/other',
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
  KEY `idx_input_type` (`input_type`),
  KEY `idx_default_supplier` (`default_supplier_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Input item master (Phase 4)';

-- ============================================================================
-- 5. HARVEST & BATCH
-- ============================================================================

CREATE TABLE `harvest_record` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`         VARCHAR(32)   NOT NULL UNIQUE COMMENT 'HV-20260520-001',
  `client_uuid`  VARCHAR(64)   UNIQUE,
  `plot_id`      BIGINT        NOT NULL,
  `plan_id`      BIGINT        NOT NULL,
  `crop_id`      BIGINT        NOT NULL,
  `variety_id`   BIGINT,
  `batch_id`     BIGINT        NOT NULL COMMENT 'Auto-generated batch',
  `harvest_date` DATE          NOT NULL,
  `qty_kg`       DECIMAL(12,3) NOT NULL CHECK (`qty_kg` > 0),
  `location_gps` VARCHAR(64)   NULL COMMENT 'GPS lat,lng captured at harvest time',
  `operator_id`  BIGINT        NOT NULL,
  `photos`       JSON,
  `remark`       VARCHAR(255),
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`   BIGINT,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_plot_date` (`plot_id`,`harvest_date`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB COMMENT='Harvest record';

CREATE TABLE `batch` (
  `id`                BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`              VARCHAR(32)   NOT NULL UNIQUE COMMENT 'B-20260520-P001-01',
  `parent_batch_id`   BIGINT        NULL COMMENT 'Split scenario',
  `plot_id`           BIGINT        NOT NULL,
  `plan_id`           BIGINT        NOT NULL,
  `crop_id`           BIGINT        NOT NULL,
  `variety_id`       BIGINT,
  `harvest_record_id` BIGINT        NOT NULL,
  `harvest_date`      DATE          NOT NULL,
  `qty_kg`            DECIMAL(12,3) NOT NULL,
  `qty_remain_kg`     DECIMAL(12,3) NOT NULL COMMENT 'Remaining unprocessed qty',
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'pending'
                      COMMENT 'pending/processing/packed/sold_out/lost',
  `remark`            VARCHAR(255),
  `created_at`        DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`        BIGINT,
  `updated_at`        DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`        DATETIME      NULL,
  KEY `idx_plot_date` (`plot_id`,`harvest_date`),
  KEY `idx_status` (`status`),
  KEY `idx_code` (`code`),
  KEY `idx_parent` (`parent_batch_id`)
) ENGINE=InnoDB COMMENT='Batch - full-chain key';

-- ============================================================================
-- 6. PACKHOUSE & INVENTORY
-- ============================================================================

CREATE TABLE `grading` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `batch_id`     BIGINT        NOT NULL,
  `grade_date`   DATE          NOT NULL,
  `qty_a_kg`     DECIMAL(12,3) NOT NULL DEFAULT 0,
  `qty_b_kg`     DECIMAL(12,3) NOT NULL DEFAULT 0,
  `qty_c_kg`     DECIMAL(12,3) NOT NULL DEFAULT 0,
  `qty_loss_kg`  DECIMAL(12,3) NOT NULL DEFAULT 0,
  `operator_id`  BIGINT        NOT NULL,
  `remark`       VARCHAR(255),
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`   BIGINT,
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB COMMENT='Grading';

CREATE TABLE `sku` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`          VARCHAR(64)  NOT NULL UNIQUE,
  `name`          VARCHAR(128) NOT NULL,
  `crop_id`       BIGINT       NOT NULL,
  `variety_id`    BIGINT,
  `grade`         VARCHAR(8)   NOT NULL,
  `spec_id`       BIGINT       NOT NULL,
  `unit`          VARCHAR(8)   NOT NULL DEFAULT 'pack',
  `status`        TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dim` (`crop_id`,`variety_id`,`grade`,`spec_id`)
) ENGINE=InnoDB COMMENT='SKU products';

CREATE TABLE `packing` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`          VARCHAR(32)   NOT NULL UNIQUE COMMENT 'PK-20260520-001',
  `batch_id`     BIGINT        NOT NULL,
  `grade`         VARCHAR(8)    NOT NULL,
  `spec_id`       BIGINT        NOT NULL,
  `sku_id`        BIGINT        NOT NULL,
  `qty_units`     INT           NOT NULL CHECK (`qty_units` > 0),
  `net_weight_kg` DECIMAL(12,3) NOT NULL,
  `location_id`   BIGINT        NOT NULL,
  `packed_at`     DATETIME      NOT NULL,
  `operator_id`   BIGINT        NOT NULL,
  `remark`        VARCHAR(255),
  `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_batch` (`batch_id`),
  KEY `idx_sku` (`sku_id`),
  KEY `idx_packed_at` (`packed_at`)
) ENGINE=InnoDB COMMENT='Packing';

CREATE TABLE `inventory` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `sku_id`          BIGINT        NOT NULL,
  `batch_id`        BIGINT        NOT NULL,
  `grade`           VARCHAR(8)    NOT NULL,
  `location_id`     BIGINT        NOT NULL,
  `qty_avail`       DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT 'Available',
  `qty_locked`      DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT 'Locked',
  `qty_in_transit`  DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT 'In transit',
  `unit`            VARCHAR(8)    NOT NULL DEFAULT 'pack',
  `prod_date`       DATE          NOT NULL COMMENT 'Production date (inherited from batch)',
  `expiry_date`     DATE          NULL DEFAULT NULL COMMENT 'Best-before = pack_date + shelf_life; drives FEFO',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'normal'
                    COMMENT 'normal/frozen/lost',
  `version`         INT           NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `last_op_at`      DATETIME,
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sku_batch_loc` (`sku_id`,`batch_id`,`grade`,`location_id`),
  KEY `idx_sku_avail` (`sku_id`,`qty_avail`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_prod_date` (`prod_date`),
  KEY `idx_expiry` (`expiry_date`)
) ENGINE=InnoDB COMMENT='Inventory - operational node';

CREATE TABLE `inventory_adjust_log` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `inventory_id`  BIGINT        NOT NULL,
  `adjust_type`   VARCHAR(16)   NOT NULL COMMENT 'in/out/lock/unlock/loss/audit',
  `reason_code`   VARCHAR(32)   COMMENT 'count_gain/count_loss/damage/manual',
  `qty_before`    DECIMAL(12,3) NOT NULL,
  `qty_change`    DECIMAL(12,3) NOT NULL,
  `qty_after`     DECIMAL(12,3) NOT NULL,
  `field_name`    VARCHAR(32)   NOT NULL COMMENT 'qty_avail/qty_locked/...',
  `ref_type`      VARCHAR(32)   COMMENT 'order/fulfillment/packing/manual',
  `ref_id`        BIGINT,
  `remark`        VARCHAR(255),
  `operator_id`   BIGINT        NOT NULL,
  `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_inv_time` (`inventory_id`,`created_at`),
  KEY `idx_ref` (`ref_type`,`ref_id`)
) ENGINE=InnoDB COMMENT='Inventory adjust log';

CREATE TABLE `loss_record` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `batch_id`     BIGINT        NOT NULL,
  `stage`        VARCHAR(16)   NOT NULL COMMENT 'grading/packing/inventory/other',
  `reason_code`  VARCHAR(32)   NOT NULL,
  `qty_kg`       DECIMAL(12,3) NOT NULL,
  `occur_at`     DATETIME      NOT NULL,
  `operator_id`  BIGINT        NOT NULL,
  `remark`       VARCHAR(255),
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_batch` (`batch_id`),
  KEY `idx_occur` (`occur_at`)
) ENGINE=InnoDB COMMENT='Loss record';

-- ============================================================================
-- 7. SALES
-- ============================================================================

CREATE TABLE `customer` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`          VARCHAR(32)  NOT NULL UNIQUE,
  `name`          VARCHAR(128) NOT NULL,
  `type`          VARCHAR(32)  NOT NULL COMMENT 'supermarket/restaurant/ecommerce/...',
  `contact_name`  VARCHAR(64),
  `contact_phone` VARCHAR(20),
  `credit_level`  VARCHAR(8)   COMMENT 'A/B/C/D',
  `credit_days`   INT          NOT NULL DEFAULT 0 COMMENT 'Credit term days: 0=COD, 7=Weekly, 30=Monthly',
  `payment_terms` VARCHAR(32)  COMMENT 'Payment terms label: COD / Weekly / Monthly / Net 30',
  `since_date`    DATE,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'active',
  `remark`        VARCHAR(255),
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `created_by`    BIGINT,
  `updated_at`    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     NULL,
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Customer';

CREATE TABLE `customer_address` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `customer_id`  BIGINT       NOT NULL,
  `recipient`    VARCHAR(64)  NOT NULL,
  `phone`        VARCHAR(20),
  `province`     VARCHAR(64),
  `city`         VARCHAR(64),
  `district`     VARCHAR(64),
  `address_line` VARCHAR(255) NOT NULL,
  `is_default`   TINYINT(1)   NOT NULL DEFAULT 0,
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB COMMENT='Customer address book';

CREATE TABLE `sales_order` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE COMMENT 'SO-20260520-001',
  `customer_id`    BIGINT        NOT NULL,
  `order_date`     DATE          NOT NULL,
  `delivery_date`  DATE          NOT NULL,
  `ship_to`        VARCHAR(255)  NOT NULL,
  `currency`       VARCHAR(8)    NOT NULL DEFAULT 'KES' COMMENT 'KES / USD / EUR',
  `total_amount`   DECIMAL(14,2) NOT NULL DEFAULT 0,
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'pending'
                   COMMENT 'pending/confirmed/locked/shipping/shipped/delivered/completed/cancelled/returned',
  `payment_status` VARCHAR(16)   NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid/partial/paid',
  `paid_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'Cumulative received amount (KES)',
  `due_date`       DATE          COMMENT 'Due date = order_date + customer.credit_days',
  `remark`         VARCHAR(500),
  `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`     BIGINT,
  `deleted_at`     DATETIME      NULL,
  KEY `idx_customer_status` (`customer_id`,`status`),
  KEY `idx_date_status` (`order_date`,`status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Sales order';

CREATE TABLE `order_item` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `order_id`      BIGINT        NOT NULL,
  `sku_id`        BIGINT        NOT NULL,
  `qty`           DECIMAL(12,3) NOT NULL,
  `unit_price`    DECIMAL(12,2) NOT NULL,
  `amount`        DECIMAL(14,2) NOT NULL,
  `qty_shipped`   DECIMAL(12,3) NOT NULL DEFAULT 0,
  `remark`        VARCHAR(255),
  `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_order` (`order_id`),
  KEY `idx_sku` (`sku_id`)
) ENGINE=InnoDB COMMENT='Order line';

CREATE TABLE `order_inventory_lock` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `order_id`     BIGINT        NOT NULL,
  `order_item_id` BIGINT       NOT NULL,
  `inventory_id` BIGINT        NOT NULL,
  `qty_locked`   DECIMAL(12,3) NOT NULL,
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'locked' COMMENT 'locked/released/shipped',
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_order` (`order_id`),
  KEY `idx_inv` (`inventory_id`),
  KEY `idx_item` (`order_item_id`)
) ENGINE=InnoDB COMMENT='Order-inventory lock mapping';

CREATE TABLE `fulfillment` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`         VARCHAR(32)   NOT NULL UNIQUE COMMENT 'SH-20260520-001',
  `order_id`     BIGINT        NOT NULL,
  `picker_id`    BIGINT,
  `plan_ship_at` DATETIME,
  `ship_at`      DATETIME,
  `delivered_at` DATETIME,
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'pending'
                 COMMENT 'pending/picking/ready/shipped/delivered/cancelled',
  `ship_method`  VARCHAR(16)   COMMENT 'self/logistics',
  `track_no`     VARCHAR(64),
  `driver_name`  VARCHAR(64),
  `driver_phone` VARCHAR(20),
  `vehicle_no`   VARCHAR(32),
  `remark`       VARCHAR(255),
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`   BIGINT,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_order` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Outbound order';

CREATE TABLE `fulfillment_item` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `fulfillment_id` BIGINT       NOT NULL,
  `order_item_id` BIGINT        NOT NULL,
  `inventory_id`  BIGINT        NOT NULL,
  `batch_id`      BIGINT        NOT NULL COMMENT 'Denormalized for faster traceability',
  `sku_id`        BIGINT        NOT NULL,
  `qty`           DECIMAL(12,3) NOT NULL,
  `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ful` (`fulfillment_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_inv` (`inventory_id`)
) ENGINE=InnoDB COMMENT='Outbound line items - with traceability';

CREATE TABLE `payment` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32),
  `order_id`        BIGINT        NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `amount`          DECIMAL(14,2) NOT NULL,
  `currency`        VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) DEFAULT 1.0,
  `amount_kes`      DECIMAL(14,2) NOT NULL,
  `method`          VARCHAR(16)   NOT NULL COMMENT 'cash / bank / cheque / loop_online / loop_pos',
  `payment_date`    DATE          NOT NULL,
  `reference_no`    VARCHAR(64),
  `pos_terminal_id` VARCHAR(64)   COMMENT 'POS terminal / Till Number (loop_pos)',
  `channel`         VARCHAR(32)   COMMENT 'Loop internal channel: mpesa/card/bank',
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
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='Payment receipt log';

CREATE TABLE `revenue` (
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
) ENGINE=InnoDB COMMENT='Revenue log - V2.0 P&L fact table';

CREATE TABLE `action_item` (
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
) ENGINE=InnoDB COMMENT='Action items - Sprint 10 decision center';

-- ============================================================================
-- 7.5  COMPLAINT & RECALL (Sprint 27)
-- ============================================================================

CREATE TABLE `complaint` (
  `id`                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `code`                VARCHAR(32)    NOT NULL UNIQUE,
  `reported_at`         DATETIME       NOT NULL,
  `customer_id`         BIGINT         NULL,
  `order_id`            BIGINT         NULL,
  `batch_id`            BIGINT         NULL,
  `sku_id`              BIGINT         NULL,
  `category`            VARCHAR(32)    NOT NULL COMMENT 'quality / quantity / late / safety / wrong_product / other',
  `severity`            VARCHAR(16)    NOT NULL DEFAULT 'medium',
  `channel`             VARCHAR(16)    NOT NULL DEFAULT 'phone',
  `description`         TEXT           NOT NULL,
  `photo_ids`           JSON           NULL,
  `status`              VARCHAR(24)    NOT NULL DEFAULT 'open'
                                       COMMENT 'open / investigating / resolved / closed / escalated_to_recall',
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

CREATE TABLE `recall` (
  `id`                      BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`                    VARCHAR(32)  NOT NULL UNIQUE,
  `triggered_at`            DATETIME     NOT NULL,
  `source_complaint_id`     BIGINT       NULL,
  `batch_id`                BIGINT       NOT NULL,
  `scope`                   VARCHAR(24)  NOT NULL DEFAULT 'batch_only',
  `reason`                  TEXT         NOT NULL,
  `status`                  VARCHAR(24)  NOT NULL DEFAULT 'initiated'
                                         COMMENT 'initiated / quarantined / customers_notified / closed',
  `affected_order_count`    INT          NOT NULL DEFAULT 0,
  `affected_customer_count` INT          NOT NULL DEFAULT 0,
  `affected_qty`            DECIMAL(14,3) NOT NULL DEFAULT 0,
  `initiated_by_id`         BIGINT       NULL,
  `closed_at`               DATETIME     NULL,
  `closed_by_id`            BIGINT       NULL,
  `closed_remark`           VARCHAR(500) NULL,
  `created_at`              DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_batch`           (`batch_id`),
  KEY `idx_status`          (`status`),
  KEY `idx_triggered_at`    (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Batch recall';

CREATE TABLE `recall_affected_order` (
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

-- ============================================================================
-- 8. DAILY REPORT (轻量)
-- ============================================================================

CREATE TABLE `daily_report` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `staff_id`        BIGINT       NOT NULL,
  `report_date`     DATE         NOT NULL,
  `done_summary`    VARCHAR(500),
  `issues`          VARCHAR(500),
  `next_plan`       VARCHAR(500),
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_staff_date` (`staff_id`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily report - lightweight';

-- ============================================================================
-- 9. SERVICE MODULE (Sprint 40 - AgriOS ↔ Chatwoot bridge, minimal)
-- ============================================================================

CREATE TABLE `service_contact_link` (
  `id`                       BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `agrios_entity_type`       VARCHAR(32)  NOT NULL DEFAULT 'customer',
  `agrios_entity_id`         BIGINT       NOT NULL,
  `chatwoot_contact_id`      BIGINT       NOT NULL,
  `chatwoot_account_id`      BIGINT       NOT NULL,
  `last_synced_at`           DATETIME     NULL,
  `sync_status`              VARCHAR(16)  NOT NULL DEFAULT 'pending',
  `sync_error`               VARCHAR(500) NULL,
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_chatwoot_contact` (`chatwoot_account_id`, `chatwoot_contact_id`),
  KEY `idx_agrios_entity` (`agrios_entity_type`, `agrios_entity_id`),
  KEY `idx_sync_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Service module - AgriOS entity to Chatwoot contact bridge';

CREATE TABLE `service_event_log` (
  `id`                       BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `event_type`               VARCHAR(64)  NOT NULL,
  `direction`                VARCHAR(16)  NOT NULL,
  `agrios_entity_type`       VARCHAR(32)  NULL,
  `agrios_entity_id`         BIGINT       NULL,
  `chatwoot_account_id`      BIGINT       NULL,
  `chatwoot_conversation_id` BIGINT       NULL,
  `chatwoot_message_id`      BIGINT       NULL,
  `payload`                  JSON         NULL,
  `result`                   VARCHAR(16)  NOT NULL DEFAULT 'ok',
  `error_message`            VARCHAR(500) NULL,
  `idempotency_key`          VARCHAR(128) NULL,
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_idempotency` (`idempotency_key`),
  KEY `idx_agrios_entity` (`agrios_entity_type`, `agrios_entity_id`, `created_at`),
  KEY `idx_chatwoot_conv` (`chatwoot_conversation_id`, `created_at`),
  KEY `idx_event_type` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Service module - cross-system event audit log';
