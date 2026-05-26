-- ============================================================================
-- Sprint 9.2 migration:
--   Add currency column to sales_order.
--   Skip if column already present (idempotent).
-- ============================================================================

ALTER TABLE `sales_order`
  ADD COLUMN `currency` VARCHAR(8) NOT NULL DEFAULT 'KES'
  COMMENT 'KES / USD / EUR'
  AFTER `ship_to`;
