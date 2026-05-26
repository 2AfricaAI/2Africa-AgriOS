-- ============================================================================
-- Sprint 14.2 - Payment 表 + AR 应收账款
--   一个 order 可以收 N 次款 (定金 + 尾款 + 退款 ...), payment.amount 累计
--   AR 应收 = order.total_amount - SUM(payment.amount where order_id = ?)
--   Sprint 15 M-Pesa 集成时, Daraja API 回调写入 payment row (method=m-pesa)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `payment` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   COMMENT 'PAY-yyyyMMdd-NNN (可空, 第三方回调可能没有)',
  `order_id`        BIGINT        NOT NULL,
  `customer_id`     BIGINT        NOT NULL,
  `amount`          DECIMAL(14,2) NOT NULL,
  `currency`        VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `fx_rate`         DECIMAL(12,6) DEFAULT 1.0 COMMENT '对 KES 换算汇率',
  `amount_kes`      DECIMAL(14,2) NOT NULL COMMENT '本位币 KES 金额',
  `method`          VARCHAR(16)   NOT NULL COMMENT 'cash / m-pesa / bank / cheque',
  `payment_date`    DATE          NOT NULL,
  `reference_no`    VARCHAR(64)   COMMENT 'M-Pesa 回执号 / 银行流水 / 支票号',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'cleared'
                    COMMENT 'pending / partial / cleared / bad_debt / reversed',
  `reconciled_by`   BIGINT        COMMENT '对账人',
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
) ENGINE=InnoDB COMMENT='回款流水 - 含 M-Pesa / 银行 / 现金 / 支票';
