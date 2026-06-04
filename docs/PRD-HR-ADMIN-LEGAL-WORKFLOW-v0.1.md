# 2Africa AgriOS — HR / 行政 / 法务合规 / 审批流 整体规划 PRD v0.1

> 编写：2026-06 · 范围：Sprint 51 ~ 60（约 8 周）
> 目标：在「底线风险管控」前提下做到「效率最大化」。
> 上下文：现有 13 个模块（master / production / warehouse / packhouse / sales / finance / qc / ops / procurement / service / system / framework / common）已上线，本规划是对其的**横向扩展**，不替换现有任何流程。

---

## 0. 三条设计底线（先于一切功能）

| 底线 | 含义 | 不可妥协 |
|---|---|---|
| **审计不可篡改** | 所有"钱、权、人"动作必须落 append-only 日志，DBA 也不能改 | 单独的 `audit_log` 表 + 触发器写入 + 每月归档到 MinIO 冷存 |
| **职责分离 SoD** | 谁发起 ≠ 谁审批 ≠ 谁支付 | 一行 SQL 校验：`prepared_by ≠ approved_by ≠ paid_by`；违反就拒绝事务 |
| **PII 最小可见** | 身份证号 / 银行卡 / 工资额按需可见，不进 list 接口 | 字段级 `@MaskOnSerialize`，列表只回脱敏；详情页 + 审计日志后才解密 |

**任何功能设计冲突这三条都得让步**，包括"用户体验更顺滑"。这是核心原则，不是建议。

---

## 1. 总体架构：4 个垂直模块 + 1 个横向能力

```
                     ┌─────────────────────────────────────────┐
                     │      WORKFLOW (审批引擎 横向能力)        │
                     │  wf_definition / wf_instance / wf_step  │
                     └────────┬─────────────┬─────────┬────────┘
                              │             │         │
              ┌───────────────▼───┐  ┌──────▼──┐ ┌────▼──────┐
              │       HR          │  │  ADMIN  │ │  LEGAL    │
              │  员工/出勤/工资   │  │ 资产/费 │ │ 牌照/合同 │
              │  请假/培训        │  │ 用/文档 │ │ 审计/合规 │
              └─────────┬─────────┘  └────┬────┘ └─────┬─────┘
                        │                 │            │
                        └───────┬─────────┴────────────┘
                                ▼
                  复用既有：sys_user / activity / harvest
                  finance.cost / ops.rule / Africastalking / MinIO
```

**为什么审批是横向不是垂直**：现有 `activity.audit_status` 已经踩坑了 — 每个模块各自实现审批，导致 5 套不一样的审批 UI、5 个超时策略、5 份并发不一致。横向之后，HR 请假、Admin 报销、Legal 用印、Finance 付款、Sales 大额订单全走同一套引擎，只是 `wf_definition.module = 'hr' / 'admin' / ...` 不同。

---

## 2. WORKFLOW 引擎（Sprint 51-52，5 天，**第一优先级**）

> 这必须**最先做**。后面所有模块都依赖它。先建一个空架子大家用，比后面 5 套各自重做强 10 倍。

### 2.1 数据模型（最小集）

```sql
-- 模板：一种业务一个定义。可由超管在 UI 配置，不必改代码
wf_definition (
  id, code, name, module,           -- 'hr.leave', 'admin.expense', etc.
  schema_json,                       -- 表单字段 + 节点 + 规则（JSON DSL）
  version, active,
  created_by, created_at
)

-- 实例：用户发起一次
wf_instance (
  id, definition_id, biz_table, biz_id,   -- 反查到业务表
  status,                                  -- pending/approved/rejected/cancelled
  initiator_id, current_step_id,
  amount_hint, urgency,                    -- 给金额阈值/SLA 计算
  created_at, completed_at
)

-- 节点：每一步审批
wf_step (
  id, instance_id, seq, type,             -- 'approval' / 'cc' / 'sign' / 'pay'
  assignee_id, assignee_role,             -- 可指定人 或 指定角色
  action,                                  -- approve/reject/delegate/return
  comment, signed_at,
  sla_due_at, escalated_to_id
)

-- 审计：append-only，触发器锁死
wf_audit (
  id, instance_id, step_id, actor_id,
  action, before_json, after_json,
  ip, user_agent, occurred_at
)
```

### 2.2 关键能力

| 能力 | 实现要点 | 为什么必须 |
|---|---|---|
| **金额触发的逐级审批** | `amount_hint` ≥ 阈值，自动多接一级。阈值表 `wf_threshold` 可配 | 报销 500 KES 老板亲批 vs 50,000 KES 财务+CEO 串签 |
| **SLA + 自动升级** | 超 SLA 自动 escalate 到上级；24h 再不批 → 钉群/SMS | 防止"批文卡在某个人桌面上" |
| **并签 + 串签** | `wf_step.seq` 同号=并签，不同号=串签 | 用印场景必须串：法务先 → 财务 → CEO |
| **代签 / 委托** | A 出差，把权限暂时委托给 B，有时效 | 实际管理需要，不给就是逼用户共享密码 |
| **撤回** | 已发起未通过的可撤回；通过的不可撤回（要走撤销流程） | SoD 底线 |
| **多端通知** | 站内消息 + 邮件 + SMS（用 Africastalking）+ WhatsApp（用 Chatwoot 出站） | 蓝领工人无邮箱，必须 SMS |

### 2.3 嵌入式 UI（关键效率点）

**不**做独立"待办收件箱"页面。原因：用户每天要工作的页面是各自模块的页面，再多一个收件箱页他不会去看。

做法：每个模块页右上角一个 badge，点开是该模块下的我的待批；详情页一键 approve/reject，不跳转。**待办引导用户回到业务上下文**，而不是脱离业务上下文。

---

## 3. HR 模块（Sprint 53-56，10 天）

### 3.1 范围 + 与现有模块的接缝

| 子域 | 表 | 复用现有 | 风险等级 |
|---|---|---|---|
| 员工档案 | `hr_employee`（1:1 ↔ sys_user） | sys_user 账号体系 | **高**（PII） |
| 合同 | `hr_contract`（PDF 存 MinIO，关键字段冗余） | MinIO 文档桶 | **高**（合规） |
| 出勤 | `hr_attendance`（GPS + 时间戳） | production.activity.operator_id | 中 |
| 计件/计时工资 | `hr_payroll_item`（每次工作的应得） | harvest_record.qty_kg、activity 工时 | **高**（钱） |
| 月薪汇总 | `hr_payroll_run`（每月一次，含 PAYE/NHIF/NSSF） | finance.cost（人工成本） | **高**（钱+合规） |
| 发薪 | `hr_payment`（走 M-Pesa B2C） | sales 的 STK Push 框架 | **高**（钱） |
| 请假 | `hr_leave_request`（→ wf_instance） | workflow 引擎 | 中 |
| 培训 | `hr_training_record` | ops.rule（培训到期提醒） | 低（但农药证关合规） |

### 3.2 三个关键设计决策

**决策 1：合同 PDF 存档 + 关键字段冗余**

合同 PDF 进 MinIO（带版本 + WORM 不可删），但 `start_date / end_date / monthly_base / role / probation_until` 等关键字段必须**冗余**进结构化表。

为什么：合同到期提醒、工资计算、合规审计都要查询，从 PDF OCR 不靠谱。**结构化表是事实之源，PDF 是法律证据**，两者并存。

**决策 2：计件工资源于已有 harvest_record，不重复录入**

农忙采收，一个工人一天可能录 20 次 harvest_record。再让他录 20 次"今天我采了多少"就是双倍劳动 + 不一致。

做法：`hr_payroll_item` 提供 view `v_hr_pieces_from_harvest`，按 `operator_id + date` 聚合 `harvest_record.qty_kg`，乘 `hr_rate_card.price_per_kg` 即得当日应得。工资单生成时一次性物化。

**决策 3：发薪走 M-Pesa B2C + 双人确认 + 金额上限**

蓝领工人没银行卡，发薪只能走 M-Pesa。但 B2C API 一旦请求就不可撤销。**必须**：
- 单笔上限 + 月度总额上限（硬编码 + 可配）
- 双人在线确认（一个发起 + 一个在另一台设备 OTP 确认）
- 任何超阈值走 workflow 引擎
- 发薪记录立刻进 audit_log
- 任何外发短信里只显示金额尾数（防止短信被截图扩散）

### 3.3 PII 字段分类（必须实施）

| 字段 | 等级 | 存储 | 列表接口 | 详情接口 | 解锁条件 |
|---|---|---|---|---|---|
| 姓名、工号、电话 | L1 公开 | 明文 | 明文 | 明文 | - |
| 银行卡号 | L2 内部 | AES-GCM | `****1234` | 明文 | 仅 HR + 写 audit_log |
| 身份证号 | L3 受限 | AES-GCM | `****` | `XXX***1234` | 仅 HR Director + 写 audit_log + 给原因 |
| 工资金额 | L2 内部 | 明文 | `***` | 明文 | 仅本人 + HR + 写 audit_log |
| 合同 PDF | L3 受限 | MinIO 私有桶 + 短期 URL | - | 短期 URL | 仅本人 + HR + 写 audit_log |

**实施手段**：Jackson 自定义 `@MaskOnSerialize` + `LoggedDecryptor` AOP 切面，违反就抛 `SecurityException` 而不是返回脱敏值（让开发期就暴露问题）。

---

## 4. 行政模块（Sprint 57-58，4 天）

### 4.1 范围

| 子域 | 表 | 复用 | 重点 |
|---|---|---|---|
| 资产 | `admin_asset`（农机/车/大棚） | ops.rule | 维保到期提醒、报废走 workflow |
| 维保记录 | `admin_maintenance_log` | - | 平时手工录，工单可走 workflow |
| 费用报销 | `admin_expense` | finance.cost、workflow | 金额触发审批，PDF 发票存 MinIO |
| 文档/SOP | `admin_document` | MinIO | 强制阅读签收（合规培训用） |
| 公告 | `admin_announcement` | system.sys_user_role 投递 | 按角色 + 强制阅读回执 |
| 牌照 | `admin_license`（食安/农药/出口） | ops.rule | **重要**：到期前 90/60/30/7 天预警 |

### 4.2 一个高 ROI 设计

**资产维保排期 + 钱包预算挂钩**

农场最常见的"坏事"是某台拖拉机突然趴窝，但其实保养记录显示它 200 工时该换机油了 100 工时前。

`admin_asset` 不只是登记表 — 它带「下次保养工时/日期」+ `ops.rule` 自动每天扫描 + 提醒到负责人手机。同时 `admin_maintenance_log` 入账自动 → `finance.cost` 配资产 → 资产折旧 → CFO 视图。

一个表三个客户（操作工 / 财务 / CFO），把单点维护变全链路可视。

---

## 5. 法务与合规模块（Sprint 59-60，4 天）

### 5.1 范围（不做"法务系统"，做"合规守护"）

> 我们不可能在 4 天内做出一个律所级法务系统。重点是**风险拦截 + 证据保全**两件事。

| 子域 | 做什么 | 不做什么 |
|---|---|---|
| 合同库 | `legal_contract` 把所有 PDF 集中 + 关键字段（对方 / 起止 / 金额 / 续约条件）冗余 | 不做合同起草 / 红线对比 |
| 续约提醒 | 复用 ops.rule，到期前 60/30/7 天提醒法务 + 业务 | 不做自动续约 |
| 牌照合规 | 复用 admin_license，但**专设**一个 dashboard 给法务总览 | 不做申报代办 |
| 数据主体请求 | DPA 2019 要求：员工/客户索查/删除自己数据，14 天内响应 | 做 endpoint + 工单，不做完全自动化 |
| 审计追溯 | 已有 `wf_audit` + 新增 `system_audit`（登录、敏感数据查看） | 不做 SIEM 集成 |
| 内部举报 | 一个匿名收件箱，HR + 法务双人收 | 不做调查工作流 |

### 5.2 肯尼亚合规清单（实施时挂钩）

| 法规 | 关键义务 | 系统实现 |
|---|---|---|
| **Employment Act 2007** | 合同 / 解雇通知 / 终结报告 | hr_contract + workflow（解雇要双签） |
| **Data Protection Act 2019** | 数据主体权 + 14 天响应 + 跨境传输报备 | 数据主体请求工单 + audit_log |
| **KRA PAYE / NHIF / NSSF** | 按法定费率每月扣缴 + 报送 | hr_payroll_run 内置费率表（可改） |
| **OSHA 2007** | 劳保 + 事故报告 | admin_document SOP + hr_training_record |
| **PCPB 农药管理** | 用药记录 + 操作员持证 | qc.inspection + hr_training_record |
| **KEPHIS 出口** | 检疫证 + 出口注册 | admin_license + qc.complaint |
| **KEBS 食安** | 食安证 + 工人健康证 | admin_license + hr_employee.health_cert_until |
| **反贿赂** | 礼品/招待 > 阈值需申报 | admin_expense + workflow 强制审批 |

---

## 6. 风险控制矩阵（落到代码层面）

| 风险点 | 控制手段 | 实现层 |
|---|---|---|
| 工资被改 | 工资单生成后 hash 入 audit_log，发薪前比对 | DB trigger + Java service |
| 一人发起+一人审批+一人支付被同人完成 | SoD 检查：三个 user_id 必须两两不等 | wf_step service 拒事务 |
| DBA 直改数据库 | audit_log 由 MySQL trigger 写，且 trigger 由 super 创建后 revoke 该权限 | DDL |
| 离职员工还能登录 | `hr_employee.status='left'` 触发 `sys_user.status='disabled'` | DB trigger |
| 短信泄露工资金额 | 短信模板禁止 `{amount}`，编译期校验 | SmsTemplate 验证 |
| 合同 PDF 误删 | MinIO WORM 桶 + 7 年保留 | bucket policy |
| 牌照过期没人管 | ops.rule 每天扫，过期前 90/60/30/7/1 全通知 | 已有 framework |
| PII 大批拉取（拖库） | 接口限速 + 同一 user 同表查 > N 行触发告警 | Spring Security 后置 |

---

## 7. Sprint 路线图（10 周）

```
Week 1-2  Sprint 51-52  Workflow 引擎（DSL + UI 嵌入 + SLA + 委托）
                        必须最先做
Week 3-4  Sprint 53-54  HR 核心 P0（员工档案 + 合同 + 出勤 + 计件工资）
                        合同 PDF + PII 加密这两件是底线
Week 5    Sprint 55     HR 月薪汇总 + M-Pesa B2C 发薪
                        双人在线确认 + SoD 必须验证通过才发版
Week 6    Sprint 56     HR 请假 / 培训 / 调休（走 workflow）
Week 7    Sprint 57     Admin 资产 + 维保 + 费用报销
                        资产-保养-成本三表联动
Week 8    Sprint 58     Admin 文档 SOP + 公告 + 牌照
Week 9    Sprint 59     法务合规：合同库 + 续约 + 数据主体请求
Week 10   Sprint 60     法务合规：审计追溯总览 + 内部举报 + Wrap
                        v3.5.0 -- 整套上线
```

### 优先级原则

1. **底层先于业务**：Workflow 引擎、PII 加密 AOP、audit_log trigger 是 51-53 内必须落地的"基础设施"
2. **钱比效率重要**：工资计算 + 发薪是 P0 高风险，宁可慢两天也不能跳过 SoD 测试
3. **嵌入比独立重要**：避免再做"待办收件箱"独立页面，全部 inline 到业务页
4. **复用比新建重要**：每张新表必须问"现有哪张表能扩字段？" — 答案是"扩 sys_user"或"扩 activity"时优先扩

---

## 8. 不做清单（明确不做的事）

为了避免 scope creep，明确**这版不做**：

- ❌ 招聘 / ATS（招聘流程不在 HR 核心）
- ❌ 绩效考核 / KPI 系统（需求不清，留 v2）
- ❌ OKR / 战略管理（不是农场刚需）
- ❌ 复杂排班 / 班次调度（农忙临时性强，反而越自动越乱）
- ❌ 法务起草助手 / 合同模板（律所工具，不是 ERP 范畴）
- ❌ ERP 级总账对接（finance 已经够用，不要陷入会计准则）
- ❌ AI 简历筛选 / AI 法务问答（PoC 都不做，会引入新风险）

这些不是「永不做」，是「v1 不做」。

---

## 9. 度量成功（v3.5.0 上线 30 天后看）

| 指标 | 目标 |
|---|---|
| 工资准时率（每月 5 号前发到工人手机） | ≥ 95% |
| 工资争议工单数（hr_payroll 走 service） | ≤ 5%（错的发了再找回比扣错更伤信任） |
| 审批 SLA 命中率（中位 24h 内有响应） | ≥ 80% |
| 牌照过期事件 | 0（任何一张过期都算系统失效） |
| 合规审计询问响应时间 | ≤ 48h（DPA 要求 14 天，我们留余量） |
| 离职后账号未禁用 case | 0 |
| PII 不当访问告警 | 月度 ≤ 3 起 |

---

## 10. 下一步

1. **本 PRD 评审**：你 + Albert（试点客户）+ 一位本地律师，约半天
2. **法律咨询**：DPA 2019、PAYE 费率表、Employment Act 解雇程序，建议一次律师 1-2 小时把红线问清
3. **PoC 选址**：Workflow + HR 员工档案 在 Albert's Farm 跑 2 周内测，再扩
4. **资源评估**：10 周路线图按你独自开发计算；如果两个人并行，可缩 30%

确认本规划方向后，我接 Sprint 50e/50f 收尾 v3.4.0；然后 Sprint 51 起按本 PRD 推进。
