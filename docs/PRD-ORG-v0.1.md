# 2Africa AgriOS — 组织模型 PRD v0.1

> 编写：2026-06 · Sprint 51（单 sprint，5 个工作日交付）
> 评审对象：你（CTO）+ 财务 + 多农场扩张负责人 + Albert（试点）
> 决策依据：上一轮选定 **方案 C：一棵主组织树 + 多个业务标签**

---

## 1. 核心问题

| 现状 | 痛点 |
|---|---|
| 没有 `farm` / `dept` / `org_node` 实体 | 多农场扩展时数据无法切分 |
| `sys_role.data_scope = 'group'` | 18 个月悬空概念，没人定义"组"是什么 |
| `plot.owner_id` 指向 sys_user | 一块地"属于一个人"而不是"属于一个农场" |
| `warehouse` / `packhouse` 无组织归属 | 跨场共池，多场无法独立核算 |
| `sys_user.organization_name` 是字符串 | 内部组织不可结构化查询 |
| 财务 P&L 无法按"哪个农场"切分 | 多场扩张后无法做盈亏分析 |

**单一目标**：让 Albert's Farm 是「实体」，让"组"有定义，让数据权限有边界。

---

## 2. 设计原则（先于功能）

| 原则 | 含义 |
|---|---|
| **组织 = 主结构** | 一棵树解决 99% 的归属、汇报、核算、权限 |
| **标签 = 补充维度** | 跨树横切关注点用 tag 表达，不允许 tag 自己长成树 |
| **节点类型化** | `org_node.type` 决定它能承载什么（FARM 可挂 plot，DEPT 不能） |
| **历史可追溯** | 节点的"父变更""人员调动"必须有时间维度，不能就地改 |
| **零侵入迁移** | 现有 Albert's Farm 数据不能因为加组织而出问题，迁移失败可回滚 |
| **数据范围拦截器** | 业务代码不写 `WHERE node_id = ?`，由 AOP/MyBatis Interceptor 自动注入 |

---

## 3. 数据模型

### 3.1 主结构：`org_node`

```sql
CREATE TABLE org_node (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  parent_id       BIGINT NULL,                    -- root 为 NULL
  code            VARCHAR(64) NOT NULL,           -- 'AGRIOS-GROUP' / 'ALBERTS-FARM' / 'PACK-A'
  name            VARCHAR(128) NOT NULL,          -- 中英斯三语？目前用 i18n_key 引用
  type            VARCHAR(32) NOT NULL,           -- 见下方枚举
  cost_center     VARCHAR(64) NULL,               -- 财务归口，与 node 不 1:1，可共享
  manager_id      BIGINT NULL,                    -- 当前 manager，FK sys_user
  ancestors       VARCHAR(512) NULL,              -- '1/3/7'，便于子树查询 WHERE ancestors LIKE '1/3/%'
  depth           INT NOT NULL DEFAULT 0,         -- 缓存深度，便于 UI 缩进
  sort_no         INT NOT NULL DEFAULT 0,
  active          TINYINT NOT NULL DEFAULT 1,
  description     VARCHAR(500) NULL,
  -- audit
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      BIGINT NULL,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,
  updated_by      BIGINT NULL,
  deleted_at      DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_code (code),
  KEY idx_org_parent (parent_id),
  KEY idx_org_ancestors (ancestors(255))
) ENGINE=InnoDB;
```

### 3.2 节点类型枚举

| type | 物理/虚拟 | 可挂什么 | 例子 |
|---|---|---|---|
| **GROUP** | 虚 | 任何子节点 | 2Africa AgriOS Group（根） |
| **FARM** | 物理 | plot / warehouse / packhouse / dept | Albert's Farm |
| **PACKHOUSE** | 物理 | dept / team | Packhouse A |
| **WAREHOUSE** | 物理 | dept / team | Cold Storage 1 |
| **DEPT** | 虚 | dept / team | Field Ops / Finance / HR |
| **TEAM** | 虚 | （叶子） | Harvest Team 3 |
| **PROJECT** | 虚 | （叶子） | 2026Q2 Export to UAE — *项目型可用 tag 替代，看场景* |

**重要约束**（DB 层 trigger 或 service 层校验）：
- `GROUP` 必须是 root
- `FARM` 之下不能再有 `FARM`（一棵农场子树）
- `TEAM` 之下不能有任何节点
- 物理节点（FARM/PACKHOUSE/WAREHOUSE）不能挂在 DEPT 下

### 3.3 业务标签：`org_tag` + `org_node_tag`

标签解决跨树维度。**严格控制**：不允许标签嵌套（标签不是隐式树）。

```sql
CREATE TABLE org_tag (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  code        VARCHAR(64) NOT NULL,
  name        VARCHAR(128) NOT NULL,
  category    VARCHAR(32) NOT NULL,    -- 'SEASON' / 'PROJECT' / 'COMPLIANCE_ZONE'
  active      TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_code (code)
) ENGINE=InnoDB;

CREATE TABLE org_node_tag (
  node_id   BIGINT NOT NULL,
  tag_id    BIGINT NOT NULL,
  PRIMARY KEY (node_id, tag_id),
  KEY idx_ont_tag (tag_id)
) ENGINE=InnoDB;
```

**标签合理用法**：
- `SEASON.2026Q2` — 这一季节投入运营的节点
- `PROJECT.EXPORT_UAE_001` — 跨场协作的某个出口项目
- `COMPLIANCE_ZONE.PCPB_A` — 持农药 A 类证的节点
- `CERTIFICATION.GLOBAL_GAP` — 通过 GlobalGAP 认证的农场

**标签滥用边界（明确禁止）**：
- ❌ `DEPT.FINANCE`（部门是结构，不是标签）
- ❌ `MANAGER.ALBERT`（管理者是节点字段，不是标签）
- ❌ `LEVEL.SENIOR`（员工级别是 HR 字段，不是标签）
- ❌ 嵌套标签（如 `Q2 > 4 月`）

### 3.4 用户-节点关系：`org_user`

一人**可属多节点**（季节工跨场、矩阵管理双线汇报）：

```sql
CREATE TABLE org_user (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  user_id         BIGINT NOT NULL,            -- FK sys_user
  node_id         BIGINT NOT NULL,            -- FK org_node
  is_primary      TINYINT NOT NULL DEFAULT 0, -- 一人有且仅有一个 primary
  position        VARCHAR(64) NULL,           -- 'Field Lead' / 'Packhouse Operator'
  is_manager      TINYINT NOT NULL DEFAULT 0, -- 是否为该节点负责人之一（可多人）
  effective_from  DATE NOT NULL,
  effective_to    DATE NULL,                  -- NULL = 当前在岗；调岗时改这里
  remark          VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_node_active (user_id, node_id, effective_from),
  KEY idx_org_user_user (user_id),
  KEY idx_org_user_node (node_id)
) ENGINE=InnoDB;
```

**关键**：`effective_from / effective_to` 让"人员调动"有历史，不就地改。三个月后查"5 月份 Albert's Farm 都有谁"是常见审计需求。

---

## 4. 数据范围（这是用户最关心的，也是最容易做错的）

### 4.1 三档语义重新定义

原 `sys_role.data_scope` 三档现在有了真实意义：

| 档位 | 语义 | 实现 |
|---|---|---|
| `self` | 只看自己创建的 / 分配给自己的数据 | `WHERE created_by = currentUser OR assignee_id = currentUser` |
| `group` | **看自己 primary 节点的整个子树** | `WHERE node_id IN (SELECT id FROM org_node WHERE ancestors LIKE '<myAncestor>/%' OR id = <myNode>)` |
| `all` | 全公司可见（仅 SUPER_ADMIN / CFO 等） | 不加范围限制 |

### 4.2 `@DataScope` 注解 + MyBatis 拦截器

业务代码**不写**范围 SQL：

```java
// Java 业务代码
@DataScope(table = "plot", column = "node_id")
public List<Plot> listPlots(PlotQuery q) {
    return mapper.selectList(wrapper);  // 拦截器自动注入 AND node_id IN (...)
}
```

拦截器内部：
1. 读当前用户 + 用户的 max(data_scope) — 如有多个角色取最宽
2. `self` → 注入 `created_by = ?`
3. `group` → 查用户 primary node ancestors，得到子树 id 集合，注入 `IN (...)`
4. `all` → 不注入

**关键限制**：超过 1000 个子树 id 时改用 `EXISTS (SELECT 1 FROM org_node n WHERE n.id = t.node_id AND n.ancestors LIKE ...)`，避免 IN 列表撑爆。

### 4.3 跨节点数据共享的例外

季节工跨场会撞两个场景：
- **场景 A**：工人 W 在 Farm 1 做了 harvest_record，调到 Farm 2 后能不能看到自己昨天在 Farm 1 的记录？
- **场景 B**：Farm 2 经理能不能看到借调来的 W 的工资？

**默认策略**：数据归属节点不变（昨天的 harvest_record 属于 Farm 1），W 通过 `org_user` 在 Farm 1 还有效期重叠的记录就还能看；Farm 2 经理看不到（不在他子树）。

**例外白名单**：HR / 总账 / 合规可配 `data_scope_override` 跳过限制（这种情况要写 audit_log）。

---

## 5. 与现有模块的耦合点（迁移清单）

| 现有表 | 加什么字段 | 迁移策略 |
|---|---|---|
| `plot` | + `node_id BIGINT` | 全部归到 Albert's Farm 节点 |
| `warehouse` | + `node_id` | 同上 |
| `packhouse_*` | + `node_id` | 同上 |
| `sys_user` | + `primary_node_id` | 全部归到对应农场，admin 归 GROUP |
| `activity` | + `node_id`（冗余 plot 的，便于查询） | 从 plot 反查 |
| `harvest_record` | + `node_id` | 同上 |
| `sales_order` | + `node_id`（卖出方哪个农场） | 默认 Albert's Farm |
| `purchase_order` | + `node_id`（买入方哪个农场） | 默认 Albert's Farm |
| `customer` | + `served_by_node_id`（哪个农场服务） | 默认 Albert's Farm |
| `cs_conversation` | + `node_id`（客户的服务农场） | 默认 Albert's Farm |
| `cost / revenue` | + `node_id` + `cost_center` | 同上 |
| `finance.*` | 渐进式添加 | 不阻塞迁移 |

**总计约 15-20 张表加字段**。所有 ALTER 都是 `ADD COLUMN ... DEFAULT NULL`，然后 backfill 到 Albert's Farm node id，**不会破坏现有查询**。

### 5.1 迁移 SQL 大致形态

```sql
-- migration 050_org_model.sql

-- 1. 建三表
CREATE TABLE org_node (...);
CREATE TABLE org_tag (...);
CREATE TABLE org_user (...);

-- 2. 建初始树
INSERT INTO org_node (id, parent_id, code, name, type, ancestors, depth)
VALUES
  (1, NULL, 'AGRIOS-GROUP',   '2Africa AgriOS Group', 'GROUP', '',     0),
  (2, 1,    'ALBERTS-FARM',   "Albert's Farm",        'FARM',  '1',    1),
  (3, 2,    'ALBERTS-OPS',    'Field Ops',            'DEPT',  '1/2',  2),
  (4, 2,    'ALBERTS-PACK',   'Packhouse',            'PACKHOUSE','1/2', 2),
  (5, 2,    'ALBERTS-WH',     'Warehouse',            'WAREHOUSE','1/2', 2),
  (6, 1,    'HQ',             'HQ',                   'DEPT',  '1',    1),
  (7, 6,    'HQ-FINANCE',     'Finance',              'DEPT',  '1/6',  2),
  (8, 6,    'HQ-HR',          'HR',                   'DEPT',  '1/6',  2),
  (9, 6,    'HQ-LEGAL',       'Legal',                'DEPT',  '1/6',  2);

-- 3. 给业务表加字段 + backfill
ALTER TABLE plot ADD COLUMN node_id BIGINT NULL AFTER owner_id;
UPDATE plot SET node_id = 2;          -- 全归 Albert's Farm
ALTER TABLE plot ADD INDEX idx_plot_node (node_id);

-- ... 其他表同样
```

### 5.2 跨服务影响

| 服务 | 改动 |
|---|---|
| `ChatwootClient` | 无直接改动（Chatwoot 是外部桥） |
| `AnalyticsService` | 加 `@DataScope`，CS-Core analytics 按用户 primary node 切 |
| `CsatService` | `cs_csat_response` 加 `node_id`，按 conv 反推 |
| `Workflow 引擎`（待建） | 按层级自动找审批人时直接读 `org_node.manager_id` 沿 ancestors 上溯 |

---

## 6. 关键决策点（请你定）

### 6.1 多语言节点名

**问题**：节点名 `Albert's Farm` 是英文，UI 切到中文/斯瓦希里语怎么办？

**选项**：
- A. `org_node.name` 存默认值，`org_node.i18n` JSON 字段存翻译
- B. 节点名只一种语言，UI 切语言不翻译节点（农场名本身是专有名词）
- C. 引入 `org_node_i18n` 表

**建议**：**B**。农场名 `Albert's Farm` / 部门名 `Finance` 是专有名词，翻不翻无意义；类型 type 可以 i18n（"FARM" → "农场" / "Shamba"）。

### 6.2 manager_id 单人 vs 多人

**问题**：一个节点可能有多个负责人（农场长 + 副场长，都能审批）。

**选项**：
- A. `org_node.manager_id` 单人，副手通过 `org_user.is_manager=1` 表达
- B. `org_node.managers` JSON 数组
- C. 完全删 `manager_id`，统一走 `org_user.is_manager`

**建议**：**A**。manager_id 是"事实第一负责人"（汇报、审批升级首选），副手是补充。这样查询简单，UI 显示明确。

### 6.3 节点删除策略

**问题**：删 Albert's Farm 怎么办？里面 1000 个 plot 怎么处理？

**选项**：
- A. 软删（`deleted_at`），子树连带软删
- B. 硬禁删（type=FARM/PACKHOUSE 永远只能 `active=0`）
- C. 删前必须无 active 业务关联

**建议**：**B**。物理实体永远不删，只 inactive。虚节点（DEPT/TEAM）可软删。

### 6.4 跨农场调动的工资归属

**问题**：W 月初在 Farm 1，月中调到 Farm 2，月末工资属于谁？

**选项**：
- A. 按当时的 `org_user.effective_from/to` 切分，按天分摊
- B. 月底 primary node 拿全部
- C. 由 HR 月末人工指定

**建议**：**A**，硬切按天。HR Sprint 时实现，本 Sprint 不涉及。

---

## 7. 不做清单

明确**本 Sprint（也长远不做）**：

- ❌ 多树并存（单树 + 标签足够）
- ❌ 节点间任意横向关联（如 "Farm 1 与 Farm 2 业务关联" — 用标签表达）
- ❌ 节点版本化（"昨天的组织结构是什么样" — DPA 要求时再加 `org_node_history`）
- ❌ 自动绘制组织图（用 React Flow / Mermaid 直接渲染就够）
- ❌ 跨节点的复杂工作流（这是 Workflow 引擎的事）
- ❌ 把外部公司（partner / customer）放进 org tree（用 `sys_user.organization_name` 即可，不混淆）

---

## 8. Sprint 51 工时分解（5 天）

| 工作项 | 工时 | 交付 |
|---|---|---|
| **Day 1** migration 050（三表 + 初始树 + ALTER 15 张表） | 1d | SQL + 回滚脚本 |
| **Day 2** `OrgNode` / `OrgTag` / `OrgUser` entity + mapper + service + CRUD controller | 1d | OpenAPI 文档 |
| **Day 3** `@DataScope` 注解 + MyBatis 拦截器（含子树 id 缓存 Redis） | 1d | 单元测试 |
| **Day 4** 前端组织树管理页（el-tree 拖拽 + 节点详情 + 标签管理） | 1d | UI |
| **Day 5** 把现有 5 个高频 controller 加 `@DataScope`（plot/warehouse/activity/harvest/conversation）+ E2E 验证 + commit + tag v3.5.0-rc1 | 1d | 提交 |

**风险与对策**：
- MyBatis Interceptor 性能：子树 id 集合用 Redis 缓存，节点改动时失效，TTL 1h
- ALTER 表中数据量大：Albert's Farm 数据量 < 10k 行，10s 内完成
- 迁移失败：每张 ALTER 都先建 backup table，失败可 swap

---

## 9. 验收 checklist（上线前必过）

- [ ] 现有 Albert's Farm 所有页面照常工作（plot 列表、harvest、conversation、analytics 都返回数据）
- [ ] 新建一个 Farm 2 节点 + 一个 plot 挂上去，用 Farm 2 的工号登录，**只看到** Farm 2 的 plot，看不到 Albert's
- [ ] admin 用户（data_scope=all）看到两边的 plot
- [ ] 借调用户（org_user 在两个节点都有效）能看到两边数据
- [ ] 离职用户（effective_to 已过）登录可见数据为 0（且自动登出）
- [ ] 删除 Albert's Farm 节点应被拒绝（FARM 不可删，只能 inactive）
- [ ] inactive 节点不出现在树选择器 dropdown 里
- [ ] 给一个用户加多个 primary（应被拒绝）
- [ ] org_user.effective_from/to 历史完整保留，不就地改
- [ ] @DataScope 性能：单页查询 < 100ms（子树 200 节点以内）

---

## 10. 后续路线图修正

```
Sprint 51  ★ ORG 组织模型 + 数据范围拦截器   ← 本 PRD
Sprint 52  ★ Workflow 引擎 - 基础           ← 原 51
Sprint 53  ★ Workflow 引擎 - SLA/委托/嵌入   ← 原 52
Sprint 54  HR 员工档案 + 合同                ← 原 53
Sprint 55  HR 出勤 + 计件                    ← 原 54
Sprint 56  HR 月薪 + M-Pesa 发薪
Sprint 57  HR 请假 + 培训
Sprint 58  Admin 资产 + 报销
Sprint 59  Admin 文档 + 公告 + 牌照
Sprint 60  Legal 合同库 + 续约 + DPA 请求
Sprint 61  Legal 审计 + 内部举报 + Wrap → v3.5.0
```

整体延后 1 sprint，但 ORG 这 5 天**节省了后续每个 sprint 各 1-2 天的"我应该按谁的视角查""组在哪里"的纠结**，净收益 > 10 天。

---

## 11. 下一步

1. **本 PRD 评审**：你 + 财务 + Albert + 多农场扩张负责人，约 1 小时
2. **6 个决策点**（§ 6）拍板
3. **PRD 定稿 v0.2** → 合并进 HR/Admin/Legal v0.2 的"组织模型"章节
4. **Sprint 50 收尾**（push main 解 reject + 50e/50f → v3.4.0）后立即开 Sprint 51
