-- ============================================================================
-- Sprint 17 - 采购 + 供应商 + 应付账款 (镜像 Sprint 9/14 销售侧)
--   supplier            : 供应商主数据 (镜像 customer)
--   purchase_order      : 采购订单头 (镜像 sales_order)
--   purchase_order_item : 采购明细 (9 种 input_type)
--   vendor_payment      : 付供应商款 (镜像 payment)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 供应商主数据
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `supplier` (
  `id`             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`           VARCHAR(32)  NOT NULL UNIQUE        COMMENT 'SUP-NNNNN 自动生成',
  `name`           VARCHAR(120) NOT NULL,
  `type`           VARCHAR(32)  NOT NULL
                   COMMENT 'input_dealer / labor_contractor / utility / equipment / service / logistics / other',
  `tax_id`         VARCHAR(64)                         COMMENT '税号 / KRA PIN',
  `contact_name`   VARCHAR(80),
  `contact_phone`  VARCHAR(32),
  `contact_email`  VARCHAR(120),
  `address`        VARCHAR(255),

  `credit_days`    INT          NOT NULL DEFAULT 0
                   COMMENT '账期天数: 0=COD, 7=周结, 30=月结',
  `payment_terms`  VARCHAR(32)                         COMMENT '账期 label',

  `since_date`     DATE                                COMMENT '建立合作日',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'active'
                   COMMENT 'active / inactive',

  `remark`         VARCHAR(500),
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`     BIGINT,
  `updated_at`     DATETIME                            ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`     DATETIME                            COMMENT '软删',

  KEY `idx_status`  (`status`),
  KEY `idx_type`    (`type`)
) ENGINE=InnoDB COMMENT='供应商主数据 - Sprint 17';

-- ----------------------------------------------------------------------------
-- 2. 采购订单 (头)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchase_order` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL UNIQUE
                    COMMENT 'PO-YYYYMMDD-NNNN 自动生成',
  `supplier_id`     BIGINT       NOT NULL,
  `order_date`      DATE         NOT NULL,
  `expected_date`   DATE                                 COMMENT '预计到货日',

  `currency`        VARCHAR(8)   NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) NOT NULL DEFAULT 1.0   COMMENT 'to KES',
  `total_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0     COMMENT '订单金额 (currency)',

  `status`          VARCHAR(16)  NOT NULL DEFAULT 'draft'
                    COMMENT 'draft / confirmed / partial_received / received / cancelled',

  `payment_status`  VARCHAR(16)  NOT NULL DEFAULT 'unpaid'
                    COMMENT 'unpaid / partial / paid (由 VendorPaymentService 自动维护)',
  `paid_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0
                    COMMENT '累计已付款 (KES 本位币)',
  `due_date`        DATE                                 COMMENT '应付日 = order_date + supplier.credit_days',

  `remark`          VARCHAR(500),
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT,
  `updated_at`      DATETIME                             ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      DATETIME                             COMMENT '软删',

  KEY `idx_supplier`  (`supplier_id`),
  KEY `idx_status`    (`status`),
  KEY `idx_payment_status_due` (`payment_status`, `due_date`),
  KEY `idx_order_date` (`order_date`)
) ENGINE=InnoDB COMMENT='采购订单头 - Sprint 17';

-- ----------------------------------------------------------------------------
-- 3. 采购订单明细
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchase_order_item` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `po_id`        BIGINT       NOT NULL,

  `input_type`   VARCHAR(32)  NOT NULL
                 COMMENT 'labor / water / electricity / fertilizer / seed / pesticide / equipment / service / other',
  `description`  VARCHAR(255) NOT NULL                COMMENT '例: NPK 17:17:17 fertilizer 50kg',

  `quantity`     DECIMAL(14,3) NOT NULL,
  `unit`         VARCHAR(16)  NOT NULL                COMMENT 'bag / kg / L / hour / person-day / lump-sum',
  `unit_price`   DECIMAL(14,2) NOT NULL                COMMENT 'currency',
  `amount`       DECIMAL(14,2) NOT NULL                COMMENT '= quantity × unit_price',

  `received_qty` DECIMAL(14,3) NOT NULL DEFAULT 0
                 COMMENT '已收数量 (部分到货支持)',

  `remark`       VARCHAR(255),
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY `idx_po`         (`po_id`),
  KEY `idx_input_type` (`input_type`)
) ENGINE=InnoDB COMMENT='采购订单明细 - Sprint 17';

-- ----------------------------------------------------------------------------
-- 4. 付供应商款 (镜像 payment 表)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `vendor_payment` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL UNIQUE       COMMENT 'VPAY-YYYYMMDD-NNNN',
  `po_id`           BIGINT       NOT NULL,
  `supplier_id`     BIGINT       NOT NULL,

  `amount`          DECIMAL(14,2) NOT NULL,
  `currency`        VARCHAR(8)   NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) NOT NULL DEFAULT 1.0,
  `amount_kes`      DECIMAL(14,2) NOT NULL              COMMENT '本位币金额',

  `method`          VARCHAR(16)  NOT NULL
                    COMMENT 'cash / bank / cheque / loop_online / loop_pos',
  `payment_date`    DATE         NOT NULL,
  `reference_no`    VARCHAR(64)                         COMMENT '银行流水 / 支票号 / Loop 回执',
  `pos_terminal_id` VARCHAR(64)                         COMMENT 'loop_pos 终端号',
  `channel`         VARCHAR(32)                         COMMENT 'mpesa / card / bank',

  `status`          VARCHAR(16)  NOT NULL DEFAULT 'cleared'
                    COMMENT 'pending / cleared / reversed',

  `remark`          VARCHAR(255),
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT,
  `updated_at`      DATETIME                            ON UPDATE CURRENT_TIMESTAMP,

  KEY `idx_po`       (`po_id`),
  KEY `idx_supplier` (`supplier_id`, `payment_date`),
  KEY `idx_status`   (`status`)
) ENGINE=InnoDB COMMENT='付供应商款 - Sprint 17';

-- ----------------------------------------------------------------------------
-- 5. 预置 5 条演示供应商 (可选, 方便联调)
-- ----------------------------------------------------------------------------
INSERT INTO `supplier` (`code`, `name`, `type`, `contact_name`, `contact_phone`,
                        `credit_days`, `payment_terms`, `since_date`, `status`)
VALUES
  ('SUP-00001', 'Kenya Highland Seeds Ltd', 'input_dealer',  'Peter Mwangi', '+254-700-111-001', 0,  'COD',    '2025-01-15', 'active'),
  ('SUP-00002', 'Athi River Fertilizers',   'input_dealer',  'Grace Wanjiru', '+254-700-111-002', 30, '月结',   '2025-02-10', 'active'),
  ('SUP-00003', 'Nairobi Day Labor Co-op',  'labor_contractor', 'James Otieno', '+254-700-111-003', 7,  '周结', '2025-03-01', 'active'),
  ('SUP-00004', 'Kenya Power',              'utility',       'Customer Service', '+254-700-111-004', 30, '月结', '2024-12-01', 'active'),
  ('SUP-00005', 'Nairobi Water Co.',        'utility',       'Billing Office',  '+254-700-111-005', 30, '月结', '2024-12-01', 'active');
