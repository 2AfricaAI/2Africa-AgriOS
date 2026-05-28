-- ============================================================================
-- Migration 036: qc_inspection + qc_inspection_item
-- Sprint 24 / Phase 5 — Quality Control
-- ============================================================================
-- 三种质检场景统一一张表:
--   incoming     — 来料质检 (PO 收货时, ref_type='warehouse_inbound')
--   in_process   — 过程抽检 (田间/包装中, ref_type='activity' / 'planting_plan')
--   outgoing     — 成品质检 (出货前, ref_type='packing' / 'batch')
--
-- result 枚举:
--   pending           — 草稿
--   pass              — 合格放行
--   conditional_pass  — 限制合格 (需返工/补救)
--   fail              — 不合格 (拒收 / 报废)
-- ============================================================================

USE toafrica_agrios;

CREATE TABLE IF NOT EXISTS `qc_inspection` (
  `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  `code`            VARCHAR(32)   NOT NULL UNIQUE COMMENT 'QC-YYYYMMDD-NNNN',
  `inspection_type` VARCHAR(16)   NOT NULL COMMENT 'incoming / in_process / outgoing',

  `ref_type`        VARCHAR(32)   NULL COMMENT 'warehouse_inbound / activity / planting_plan / packing / batch',
  `ref_id`          BIGINT        NULL COMMENT 'ID of the referenced entity',
  `ref_code`        VARCHAR(64)   NULL COMMENT 'Snapshot code (e.g., IN-20260528-0001)',

  `inspect_date`    DATE          NOT NULL,
  `inspector_id`    BIGINT        NULL COMMENT 'FK -> sys_user.id',

  `result`          VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT 'pending / pass / conditional_pass / fail',
  `result_remark`   VARCHAR(500)  NULL COMMENT 'Overall conclusion remark',

  `photo_ids`       JSON          NULL COMMENT 'Attached photo sys_file.id array',
  `remark`          VARCHAR(500)  NULL,

  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`      BIGINT        NULL,
  `updated_at`      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,

  KEY `idx_qc_type`     (`inspection_type`),
  KEY `idx_qc_result`   (`result`),
  KEY `idx_qc_ref`      (`ref_type`, `ref_id`),
  KEY `idx_qc_date`     (`inspect_date` DESC)
) ENGINE=InnoDB COMMENT='QC inspection orders — Sprint 24';

-- 检查项明细
CREATE TABLE IF NOT EXISTS `qc_inspection_item` (
  `id`              BIGINT         PRIMARY KEY AUTO_INCREMENT,
  `inspection_id`   BIGINT         NOT NULL,

  `check_point`     VARCHAR(64)    NOT NULL COMMENT 'Check item (appearance / weight / moisture / residue ...)',
  `expected_value`  VARCHAR(128)   NULL COMMENT 'Expected value / range (e.g. 8-10% moisture)',
  `actual_value`    VARCHAR(128)   NULL COMMENT 'Measured value',
  `result`          VARCHAR(8)     NULL DEFAULT 'pending' COMMENT 'pass / fail / pending',
  `remark`          VARCHAR(255)   NULL,

  KEY `idx_qci_parent` (`inspection_id`)
) ENGINE=InnoDB COMMENT='QC inspection items — Sprint 24';
