-- ============================================================================
-- Sprint 9.4 — Revenue 表
--   每次 fulfillment 发货完成时, 按 OrderItem 粒度落一条 revenue 流水。
--   这是 V2.0 Phase 2 P&L 的核心事实表。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `revenue` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `order_id`         BIGINT        NOT NULL,
  `order_item_id`    BIGINT        NOT NULL,
  `fulfillment_id`   BIGINT        NOT NULL,
  `sku_id`           BIGINT        NOT NULL,
  `customer_id`      BIGINT        NOT NULL,
  `batch_id`         BIGINT        NULL COMMENT '主要批次 (多批合并发货时存第一批)',
  `qty`              DECIMAL(12,3) NOT NULL,
  `gross_amount`     DECIMAL(14,2) NOT NULL,
  `tax`              DECIMAL(14,2) NOT NULL DEFAULT 0,
  `net_amount`       DECIMAL(14,2) NOT NULL,
  `currency`         VARCHAR(8)    NOT NULL DEFAULT 'KES',
  `recognition_date` DATE          NOT NULL,
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'recognized'
                     COMMENT 'recognized / reversed / adjusted',
  `channel`          VARCHAR(32)   COMMENT 'b2b / retail / export / online',
  `remark`           VARCHAR(255),
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`       BIGINT,
  KEY `idx_order` (`order_id`),
  KEY `idx_customer_date` (`customer_id`, `recognition_date`),
  KEY `idx_sku_date` (`sku_id`, `recognition_date`),
  KEY `idx_date` (`recognition_date`)
) ENGINE=InnoDB COMMENT='收入流水 - V2.0 P&L 事实表';
