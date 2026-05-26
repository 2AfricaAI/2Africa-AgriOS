-- ============================================================================
-- Sprint 10 - 经营行动清单 (Operations Action Board)
--   规则引擎产出的可执行动作 (今日必做 / 本周风险 / 跟进 / 暂停)
--   upsert by (rule_code, ref_type, ref_id) — 同一条件不重复
-- ============================================================================

CREATE TABLE IF NOT EXISTS `action_item` (
  `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `rule_code`       VARCHAR(32)  NOT NULL COMMENT 'R-INV-01 / R-PROD-02 / R-CUST-01 ...',
  `severity`        VARCHAR(16)  NOT NULL DEFAULT 'medium'
                    COMMENT 'high / medium / low',
  `category`        VARCHAR(32)  NOT NULL
                    COMMENT 'today / week_risk / followup / pause',
  `title`           VARCHAR(255) NOT NULL,
  `description`     VARCHAR(1000),
  `owner_role`      VARCHAR(32)  COMMENT 'sales / packhouse / qc / finance / ceo / production',
  `ref_type`        VARCHAR(32)  COMMENT 'inventory / customer / batch / order / sku / plot',
  `ref_id`          BIGINT,
  `ref_code`        VARCHAR(64)  COMMENT '冗余 - 业务编码, 加速展示',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'open'
                    COMMENT 'open / done / dismissed / auto_resolved',
  `due_date`        DATE,
  `data_snapshot`   JSON COMMENT '触发数据快照 - 调试 + 解释性展示',
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
