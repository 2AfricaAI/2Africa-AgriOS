-- ============================================================================
-- 客户信用账期 + 订单付款状态
--   credit_days: 0=现结, 7=周结, 15=半月结, 30=月结, 自定义任意天数
--   payment_terms: 显示用 label (e.g. "Net 30", "周结")
--   订单 paid_amount / payment_status / due_date 由 PaymentService 维护
-- ============================================================================

-- 1. customer 加信用账期字段
ALTER TABLE `customer`
  ADD COLUMN `credit_days`    INT          NOT NULL DEFAULT 0
             COMMENT '信用账期天数: 0=COD, 7=weekly, 30=monthly...' AFTER `credit_level`,
  ADD COLUMN `payment_terms`  VARCHAR(32)
             COMMENT '账期 label: COD / 周结 / 月结 / Net 30 ...' AFTER `credit_days`;

-- 2. sales_order 加付款相关字段
ALTER TABLE `sales_order`
  ADD COLUMN `payment_status` VARCHAR(16) NOT NULL DEFAULT 'unpaid'
             COMMENT 'unpaid / partial / paid' AFTER `status`,
  ADD COLUMN `paid_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0
             COMMENT '累计已收款 (本位币 KES)' AFTER `payment_status`,
  ADD COLUMN `due_date`       DATE
             COMMENT '应付日 = order_date + customer.credit_days' AFTER `paid_amount`;

-- 3. 索引: 付款状态 + 应付日 (用于"逾期未付"列表)
CREATE INDEX `idx_payment_status_due` ON `sales_order`(`payment_status`, `due_date`);
