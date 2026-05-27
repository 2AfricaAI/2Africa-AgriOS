-- ============================================================================
-- Migration 030: input_stock_log - 出入库流水日志
-- Sprint 22.3 — 每一笔库存变动留一条审计记录
-- ============================================================================
-- reason_type 枚举:
--   po_receive       — PO 收货入库 (Sprint 22.4)
--   activity_consume — 农事领料出库 (Sprint 22.5)
--   stocktake_adjust — 盘点调整 (Sprint 22.6)
--   damage           — 报损 (Sprint 22.7)
--   return_in        — 客户退货入库 (Sprint 22.8)
--   transfer_in      — 调拨入库 (Sprint 22.9)
--   transfer_out     — 调拨出库 (Sprint 22.9)
--   manual           — 手工调整
--
-- reference_type + reference_id 组成多态外键:
--   purchase_order / activity / stocktake / damage_report / transfer
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `input_stock_log` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,

  `input_item_id`   BIGINT         NOT NULL
                    COMMENT 'FK -> input_item.id',
  `warehouse_id`    BIGINT         NOT NULL
                    COMMENT 'FK -> location_warehouse.id',

  `direction`       ENUM('IN','OUT') NOT NULL
                    COMMENT 'IN = inbound, OUT = outbound',
  `qty`             DECIMAL(14,3)  NOT NULL
                    COMMENT 'Always positive; direction determines sign on stock',

  `reason_type`     VARCHAR(32)    NOT NULL
                    COMMENT 'po_receive / activity_consume / stocktake_adjust / damage / return_in / transfer_in / transfer_out / manual',

  `reference_type`  VARCHAR(32)    NULL
                    COMMENT 'Polymorphic: purchase_order / activity / stocktake / damage_report / transfer',
  `reference_id`    BIGINT         NULL
                    COMMENT 'ID of the referenced entity',

  `qty_after`       DECIMAL(14,3)  NOT NULL
                    COMMENT 'Stock qty_on_hand AFTER this movement (snapshot for audit trail)',

  `operator_id`     BIGINT         NULL
                    COMMENT 'FK -> sys_user.id (who performed the action)',
  `remark`          VARCHAR(255)   NULL,

  `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY `idx_log_item`       (`input_item_id`, `created_at` DESC),
  KEY `idx_log_warehouse`  (`warehouse_id`, `created_at` DESC),
  KEY `idx_log_reason`     (`reason_type`),
  KEY `idx_log_ref`        (`reference_type`, `reference_id`)
) ENGINE=InnoDB COMMENT='Input stock movement log — Sprint 22.3 (audit, immutable)';

-- ============================================================================
-- Verify
-- ============================================================================
-- DESC input_stock_log;
-- SHOW INDEX FROM input_stock_log;
-- ============================================================================
