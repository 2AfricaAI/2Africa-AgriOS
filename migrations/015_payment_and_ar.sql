-- ============================================================================
-- Sprint 14.2 - Payment 表 + AR 应收账款
--   一个 order 可以收 N 次款 (定金 + 尾款 + 退款 ...), payment.amount 累计
--   AR 应收 = order.total_amount - SUM(payment.amount where order_id = ?)
--   Sprint 15 M-Pesa 集成时, Daraja API 回调写入 payment row (method=m-pesa)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `payment` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   COMMENT 'PAY-yyyyMMdd-NNN (nullable; some third-party callbacks omit it)',
  `order_id`        BIGINT        NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `amount`          DECIMAL(14,2) NOT NULL,
  `currency`        VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) DEFAULT 1.0 COMMENT 'FX rate to KES',
  `amount_kes`      DECIMAL(14,2) NOT NULL COMMENT 'Amount in functional currency KES',
  `method`          VARCHAR(16)   NOT NULL COMMENT 'cash / m-pesa / bank / cheque',
  `payment_date`    DATE          NOT NULL,
  `reference_no`    VARCHAR(64)   COMMENT 'M-Pesa receipt / bank reference / cheque no.',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'cleared'
                    COMMENT 'pending / partial / cleared / bad_debt / reversed',
  `reconciled_by`   BIGINT        COMMENT 'Reconciler',
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
) ENGINE=InnoDB COMMENT='Payment receipt log - cash / bank / cheque / M-Pesa';
