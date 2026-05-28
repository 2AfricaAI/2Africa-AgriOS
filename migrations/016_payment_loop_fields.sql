-- ============================================================================
-- Sprint 15.1 - 接入 Loop (NCBA) 支付
--   方式从 M-Pesa 专属重构成 Loop 聚合 (Loop 内部还能选 M-Pesa/Card/Bank)
--   - method 枚举扩展: cash / bank / cheque / loop_online / loop_pos
--   - 新增 pos_terminal_id (POS 机标识, 线下回款用)
--   - 新增 channel (Loop 内部实际通道: mpesa / card / bank, 由 webhook 写入)
-- ============================================================================

ALTER TABLE `payment`
  ADD COLUMN `pos_terminal_id` VARCHAR(64) NULL
    COMMENT 'POS identifier / Till Number (set only when loop_pos)' AFTER `reference_no`,
  ADD COLUMN `channel` VARCHAR(32) NULL
    COMMENT 'Loop aggregated channel: mpesa / card / bank — written by webhook' AFTER `pos_terminal_id`;
