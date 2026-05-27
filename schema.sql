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
) ENGINE=InnoDB COMMENT='系统用户';

CREATE TABLE `sys_role` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`       VARCHAR(64)  NOT NULL UNIQUE COMMENT 'WORKER/LEADER/PACKHOUSE/SALES/MANAGER',
  `name`       VARCHAR(64)  NOT NULL,
  `data_scope` VARCHAR(16)  NOT NULL DEFAULT 'self' COMMENT 'self/group/all',
  `remark`     VARCHAR(255),
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色';

CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB COMMENT='用户-角色';

CREATE TABLE `sys_menu` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `parent_id`  BIGINT       NOT NULL DEFAULT 0,
  `code`       VARCHAR(64)  NOT NULL UNIQUE,
  `name`       VARCHAR(64)  NOT NULL,
  `type`       VARCHAR(16)  NOT NULL COMMENT 'menu/button',
  `path`       VARCHAR(255),
  `component`  VARCHAR(255),
  `perms`      VARCHAR(255) COMMENT '权限标识 e.g. plot:list',
  `icon`       VARCHAR(64),
  `sort`       INT          NOT NULL DEFAULT 0,
  `visible`    TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜单/按钮';

CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB COMMENT='角色-菜单';

CREATE TABLE `sys_dict_type` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `dict_type`  VARCHAR(64)  NOT NULL UNIQUE,
  `dict_name`  VARCHAR(128) NOT NULL,
  `remark`     VARCHAR(255),
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='字典类型';

CREATE TABLE `sys_dict_data` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `dict_type`  VARCHAR(64)  NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(128) NOT NULL,
  `sort`       INT          NOT NULL DEFAULT 0,
  `css_class`  VARCHAR(64),
  `status`     TINYINT(1)   NOT NULL DEFAULT 1,
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB COMMENT='字典数据';

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
) ENGINE=InnoDB COMMENT='操作日志';

CREATE TABLE `sys_file` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `object_key`    VARCHAR(512) NOT NULL UNIQUE COMMENT 'MinIO/OSS 对象 key',
  `original_name` VARCHAR(255) NOT NULL COMMENT '上传时的原始文件名',
  `bucket`        VARCHAR(64)  NOT NULL,
  `size_bytes`    BIGINT       NOT NULL,
  `mime_type`     VARCHAR(128),
  `ext`           VARCHAR(16),
  `biz_type`      VARCHAR(32)  COMMENT '业务分类: avatar/crop_image/activity_photo/...',
  `uploaded_by`   BIGINT,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     NULL,
  KEY `idx_uploaded_by` (`uploaded_by`),
  KEY `idx_biz_type`    (`biz_type`),
  KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB COMMENT='文件上传记录';

CREATE TABLE `sys_code_rule` (
  `id`         BIGINT      PRIMARY KEY AUTO_INCREMENT,
  `rule_key`   VARCHAR(64) NOT NULL UNIQUE,
  `template`   VARCHAR(128) NOT NULL COMMENT 'e.g. B-{yyyyMMdd}-{plotCode}-{seq:02d}',
  `remark`     VARCHAR(255),
  `created_at` DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='编码规则';

-- ============================================================================
-- 2. MASTER DATA
-- ============================================================================

CREATE TABLE `crop` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`       VARCHAR(32)  NOT NULL UNIQUE,
  `name`       VARCHAR(64)  NOT NULL,
  `category`   VARCHAR(32)  COMMENT '叶菜/果蔬/根茎...',
  `unit`       VARCHAR(8)   NOT NULL DEFAULT 'kg',
  `cycle_days` INT          COMMENT '常规生长周期',
  `remark`     VARCHAR(255),
  `status`     TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='作物';

CREATE TABLE `variety` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `crop_id`    BIGINT       NOT NULL,
  `code`       VARCHAR(32)  NOT NULL,
  `name`       VARCHAR(64)  NOT NULL,
  `traits`     VARCHAR(255) COMMENT '特性',
  `status`     TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_crop_code` (`crop_id`,`code`),
  KEY `idx_crop` (`crop_id`)
) ENGINE=InnoDB COMMENT='品种';

CREATE TABLE `input` (
  `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL UNIQUE,
  `name`        VARCHAR(128) NOT NULL,
  `type`        VARCHAR(16)  NOT NULL COMMENT 'fertilizer/pesticide/seed/film/other',
  `unit`        VARCHAR(8)   NOT NULL,
  `supplier`    VARCHAR(128),
  `safety_days` INT          DEFAULT 0 COMMENT '安全间隔期（天）',
  `stock_qty`   DECIMAL(12,3) NOT NULL DEFAULT 0,
  `status`      TINYINT(1)   NOT NULL DEFAULT 1,
  `remark`      VARCHAR(255),
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_type` (`type`)
) ENGINE=InnoDB COMMENT='投入品';

CREATE TABLE `packaging_spec` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL UNIQUE,
  `name`            VARCHAR(64)  NOT NULL COMMENT '如 250g透明盒',
  `unit_net_kg`     DECIMAL(8,3) NOT NULL COMMENT '单件净重 kg',
  `unit_gross_kg`   DECIMAL(8,3) COMMENT '单件毛重 kg',
  `material`        VARCHAR(64),
  `status`          TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='包装规格';

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
  `user_id`      BIGINT       UNIQUE COMMENT '关联系统账户',
  `code`         VARCHAR(32)  NOT NULL UNIQUE,
  `name`         VARCHAR(64)  NOT NULL,
  `phone`        VARCHAR(20),
  `id_card`      VARCHAR(32),
  `role_type`    VARCHAR(32)  NOT NULL COMMENT 'worker/leader/packhouse/sales/manager',
  `team_code`    VARCHAR(32)  COMMENT '所属小组',
  `hire_date`    DATE,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'active',
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`   DATETIME     NULL,
  KEY `idx_role` (`role_type`),
  KEY `idx_team` (`team_code`)
) ENGINE=InnoDB COMMENT='员工';

-- ============================================================================
-- 4. PRODUCTION
-- ============================================================================

CREATE TABLE `plot` (
  `id`             BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)   NOT NULL UNIQUE COMMENT 'P-001',
  `name`           VARCHAR(64)   NOT NULL,
  `area_mu`        DECIMAL(10,2) NOT NULL CHECK (`area_mu` > 0),
  `location`       VARCHAR(128)  COMMENT '经纬度或描述',
  `soil_type`      VARCHAR(32),
  `irrigation`     VARCHAR(32),
  `owner_id`       BIGINT        NOT NULL COMMENT 'staff_id',
  `allowed_crops`  JSON          COMMENT '可种作物 id 数组',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT 'active/inactive/fallow',
  `remark`         VARCHAR(255),
  `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`     BIGINT,
  `deleted_at`     DATETIME      NULL,
  KEY `idx_owner_status` (`owner_id`,`status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='地块';

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
) ENGINE=InnoDB COMMENT='种植计划';

CREATE TABLE `activity` (
  `id`               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `client_uuid`      VARCHAR(64)  UNIQUE COMMENT '幂等键',
  `plot_id`          BIGINT       NOT NULL,
  `plan_id`          BIGINT       NOT NULL,
  `activity_type`    VARCHAR(16)  NOT NULL COMMENT 'sow/fertilize/spray/weed/water/prune/other',
  `occur_date`       DATE         NOT NULL,
  `operator_id`     BIGINT       NOT NULL COMMENT 'staff_id',
  `photos`          JSON         COMMENT 'OSS URL 数组',
  `location_gps`    VARCHAR(64),
  `remark`          VARCHAR(500),
  `labor_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '人工成本 (Sprint 11)',
  `water_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '水费',
  `electricity_cost` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '电费',
  `fertilizer_cost`  DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '肥料成本',
  `other_cost`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '其他成本',
  `cost_currency`    VARCHAR(8)    NOT NULL DEFAULT 'KES' COMMENT '本条 activity 所有成本字段共用货币',
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
) ENGINE=InnoDB COMMENT='农事记录';

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
) ENGINE=InnoDB COMMENT='农事-投入品明细';

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
) ENGINE=InnoDB COMMENT='投入品主数据 (Phase 4)';

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
  `batch_id`     BIGINT        NOT NULL COMMENT '自动生成的批次',
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
) ENGINE=InnoDB COMMENT='采收记录';

CREATE TABLE `batch` (
  `id`                BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`              VARCHAR(32)   NOT NULL UNIQUE COMMENT 'B-20260520-P001-01',
  `parent_batch_id`   BIGINT        NULL COMMENT '拆分场景',
  `plot_id`           BIGINT        NOT NULL,
  `plan_id`           BIGINT        NOT NULL,
  `crop_id`           BIGINT        NOT NULL,
  `variety_id`       BIGINT,
  `harvest_record_id` BIGINT        NOT NULL,
  `harvest_date`      DATE          NOT NULL,
  `qty_kg`            DECIMAL(12,3) NOT NULL,
  `qty_remain_kg`     DECIMAL(12,3) NOT NULL COMMENT '剩余未处理量',
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
) ENGINE=InnoDB COMMENT='批次 - 全链路主键';

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
) ENGINE=InnoDB COMMENT='分级';

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
) ENGINE=InnoDB COMMENT='SKU 商品';

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
) ENGINE=InnoDB COMMENT='包装';

CREATE TABLE `inventory` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `sku_id`          BIGINT        NOT NULL,
  `batch_id`        BIGINT        NOT NULL,
  `grade`           VARCHAR(8)    NOT NULL,
  `location_id`     BIGINT        NOT NULL,
  `qty_avail`       DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '可售',
  `qty_locked`      DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '锁定',
  `qty_in_transit`  DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '在途',
  `unit`            VARCHAR(8)    NOT NULL DEFAULT 'pack',
  `prod_date`       DATE          NOT NULL COMMENT '生产日期（继承批次）',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'normal'
                    COMMENT 'normal/frozen/lost',
  `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `last_op_at`      DATETIME,
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sku_batch_loc` (`sku_id`,`batch_id`,`grade`,`location_id`),
  KEY `idx_sku_avail` (`sku_id`,`qty_avail`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_prod_date` (`prod_date`)
) ENGINE=InnoDB COMMENT='库存 - 经营节点';

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
) ENGINE=InnoDB COMMENT='库存调整日志';

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
) ENGINE=InnoDB COMMENT='损耗记录';

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
  `credit_days`   INT          NOT NULL DEFAULT 0 COMMENT '账期天数: 0=COD, 7=周结, 30=月结',
  `payment_terms` VARCHAR(32)  COMMENT '账期 label: COD / 周结 / 月结 / Net 30',
  `since_date`    DATE,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'active',
  `remark`        VARCHAR(255),
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `created_by`    BIGINT,
  `updated_at`    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     NULL,
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='客户';

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
) ENGINE=InnoDB COMMENT='客户地址簿';

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
  `paid_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计已收款 (KES)',
  `due_date`       DATE          COMMENT '应付日 = order_date + customer.credit_days',
  `remark`         VARCHAR(500),
  `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`     BIGINT,
  `deleted_at`     DATETIME      NULL,
  KEY `idx_customer_status` (`customer_id`,`status`),
  KEY `idx_date_status` (`order_date`,`status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='销售订单';

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
) ENGINE=InnoDB COMMENT='订单行';

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
) ENGINE=InnoDB COMMENT='订单-库存锁定关系';

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
) ENGINE=InnoDB COMMENT='出库单';

CREATE TABLE `fulfillment_item` (
  `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `fulfillment_id` BIGINT       NOT NULL,
  `order_item_id` BIGINT        NOT NULL,
  `inventory_id`  BIGINT        NOT NULL,
  `batch_id`      BIGINT        NOT NULL COMMENT '冗余，追溯加速',
  `sku_id`        BIGINT        NOT NULL,
  `qty`           DECIMAL(12,3) NOT NULL,
  `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ful` (`fulfillment_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_inv` (`inventory_id`)
) ENGINE=InnoDB COMMENT='出库明细 - 含追溯链路';

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
  `pos_terminal_id` VARCHAR(64)   COMMENT 'POS 机/Till Number (loop_pos)',
  `channel`         VARCHAR(32)   COMMENT 'Loop 内部实际通道 mpesa/card/bank',
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
) ENGINE=InnoDB COMMENT='回款流水';

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
) ENGINE=InnoDB COMMENT='收入流水 - V2.0 P&L 事实表';

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
) ENGINE=InnoDB COMMENT='经营行动清单 - Sprint 10 决策中心';

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
  UNIQUE KEY `uk_staff_date` (`staff_id`,`report_date`)
) ENGINE=InnoDB COMMENT='日报';

-- ============================================================================
-- 9. INITIAL DATA
-- ============================================================================

-- 角色
INSERT INTO `sys_role` (`code`,`name`,`data_scope`,`remark`) VALUES
('SUPER_ADMIN','超级管理员','all','系统初始账号'),
('MANAGER',    '农场经理',   'all','全局权限'),
('LEADER',     '地块负责人/组长','group','本组数据'),
('PACKHOUSE',  'Packhouse 主管','all','分级包装库存'),
('SALES',      '销售/客服',  'all','客户订单出库'),
('WORKER',     '农场工人',   'self','仅本人记录');

-- 初始用户 admin / Admin@123456 (BCrypt 哈希请上线时替换)
-- worker / Admin@123456 (Sprint 20.8 移动端工人测试账号, 同密码哈希)
INSERT INTO `sys_user` (`username`,`password`,`nickname`,`status`) VALUES
-- BCrypt hash for password: Admin@123456  (cost=10)
('admin', '$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC','Administrator','active'),
('worker','$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC','John Mwangi','active');

-- 用户-角色绑定 (admin=SUPER_ADMIN, worker=WORKER role_id=6)
INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES (1,1), (2,6);

-- 字典类型
INSERT INTO `sys_dict_type` (`dict_type`,`dict_name`) VALUES
('activity_type','农事类型'),
('soil_type','土壤类型'),
('irrigation','灌溉方式'),
('input_type','投入品类型'),
('customer_type','客户类型'),
('order_status','订单状态'),
('batch_status','批次状态'),
('plan_status','种植计划状态');

-- 字典数据
INSERT INTO `sys_dict_data` (`dict_type`,`dict_label`,`dict_value`,`sort`) VALUES
('activity_type','播种','sow',1),
('activity_type','施肥','fertilize',2),
('activity_type','打药','spray',3),
('activity_type','除草','weed',4),
('activity_type','灌溉','water',5),
('activity_type','修剪','prune',6),
('activity_type','其他','other',99),
('soil_type','壤土','loam',1),
('soil_type','沙土','sand',2),
('soil_type','黏土','clay',3),
('soil_type','盐碱地','saline',4),
('irrigation','滴灌','drip',1),
('irrigation','喷灌','spray',2),
('irrigation','沟灌','furrow',3),
('input_type','肥料','fertilizer',1),
('input_type','农药','pesticide',2),
('input_type','种子','seed',3),
('input_type','地膜','film',4),
('input_type','其他','other',99),
('customer_type','商超','supermarket',1),
('customer_type','餐饮','restaurant',2),
('customer_type','电商','ecommerce',3),
('customer_type','批发','wholesale',4),
('customer_type','其他','other',99);

-- 编码规则
INSERT INTO `sys_code_rule` (`rule_key`,`template`,`remark`) VALUES
('plot',      'P-{seq:03d}',                                '地块编号'),
('plan',      'PL-{yy}-{seq:04d}',                          '种植计划'),
('harvest',   'HV-{yyyyMMdd}-{seq:03d}',                    '采收记录'),
('batch',     'B-{yyyyMMdd}-{plotCode}-{seq:02d}',          '批次号'),
('packing',   'PK-{yyyyMMdd}-{seq:03d}',                    '包装单'),
('order',     'SO-{yyyyMMdd}-{seq:03d}',                    '订单'),
('fulfillment','SH-{yyyyMMdd}-{seq:03d}',                   '出库单'),
('customer',  'CUS-{seq:05d}',                              '客户'),
('staff',     'EMP-{seq:04d}',                              '员工'),
('input',     'IN-{seq:05d}',                               '投入品'),
('crop',      'CR-{seq:03d}',                               '作物');

-- Seed crops (English - for African local staff)
INSERT INTO `crop` (`code`,`name`,`category`,`unit`,`cycle_days`) VALUES
('CR-001','Tomato',     'Vegetable',       'kg', 90),
('CR-002','Cucumber',   'Vegetable',       'kg', 60),
('CR-003','Lettuce',    'Leafy Vegetable', 'kg', 45),
('CR-004','Strawberry', 'Fruit',           'kg',120);

-- Seed varieties
INSERT INTO `variety` (`crop_id`,`code`,`name`,`traits`) VALUES
(1,'V-001','Cherry Tomato',     'Small, high sweetness'),
(1,'V-002','Provence Heritage', 'Large, juicy'),
(2,'V-001','Mini Snack',        'Short, crisp'),
(3,'V-001','Butterhead',        'Soft, buttery texture');

-- Seed packaging specs
INSERT INTO `packaging_spec` (`code`,`name`,`unit_net_kg`,`unit_gross_kg`,`material`) VALUES
('SP-250G','250g Clear Punnet', 0.250, 0.280, 'PET'),
('SP-500G','500g Resealable Bag',0.500, 0.510, 'PE'),
('SP-1KG', '1kg Gift Box',      1.000, 1.100, 'Paper + Liner'),
('SP-5KG', '5kg Crate',         5.000, 5.500, 'PP Crate');

-- 演示库位 (注意层级: W01 / W02 是顶层, A1/A2/C1 是其子节点)
INSERT INTO `location_warehouse` (`code`,`name`,`type`,`parent_id`) VALUES
('W01',    '一号包装仓',     'normal', 0),
('W01-A1', '一号仓-A1货架',  'normal', 1),
('W01-A2', '一号仓-A2货架',  'normal', 1),
('W02',    '冷藏库',         'cold',   0),
('W02-C1', '冷藏库-C1',      'cold',   4);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
