# 2Africa.AI AgriCloud — Handover from AgriOS v3.5.0-rc2

> 转交日期：2026-06-05
> 源版本：`2Africa AgriOS` git tag `v3.5.0-rc2`
> 转交目的：**VPC 版本部署** + **SAAS 版本架构与代码复用**
> 接收方：**2Africa.AI AgriCloud** 产品线

---

## 0. TL;DR

AgriOS v3.5.0-rc2 是一套**单租户、私有部署、面向单一农业企业**的农业经营操作系统。当前已落地：

- **生产 + 业务全闭环 80%**（Plot → Batch → Inventory → Order → Payment）
- **客服模块完整**（Chatwoot 桥接 + SLA + CSAT + 周报）
- **组织模型 + 数据范围拦截器**（多农场/多部门基础已铺好，但租户隔离尚未做）
- **审批流引擎核心**（5 张表 + DSL + 5 大动作 REST，UI 尚未做）
- **JWT 已瘦身**（10× 缩小）

转交给 AgriCloud 后，按你的目标分两个发行物：

| 目标 | 复用度 | 主要改造 |
|---|---|---|
| **AgriCloud-VPC**（单租户私有部署）| **95%+ 直接复用** | 只需改 docker-compose + 环境变量 + 品牌 logo + 域名 |
| **AgriCloud-SAAS**（多租户云服务）| **代码 70% 可复用，架构 90% 可参考** | 必须新增**租户层**（tenant_id 贯穿全表）+ 计费 + 自助 onboard |

---

## 1. 资产清单

### 1.1 代码仓库

| 项 | 路径 / Tag |
|---|---|
| Git 源 | `https://github.com/2AfricaAI/2Africa-AgriOS.git` |
| 转交点 | `git tag v3.5.0-rc2`（commit `<see git log>`）|
| 主分支 | `main` |
| 历史 tags | v3.0.0 / v3.1.0 / v3.3.0 / v3.4.0 / v3.5.0-rc1 / **v3.5.0-rc2** |

### 1.2 主要文档（`docs/` 目录）

| 文档 | 用途 |
|---|---|
| `PRD-AGRIOS-V2.0.docx`（external）| 顶层产品愿景（19 章）|
| `PRD-ORG-v0.2.md` | 组织模型 + 数据范围设计 |
| `PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2.md` | HR / 行政 / 法务 / 审批流规划 |
| `STATE-AT-PAUSE-2026-06-05.md` | 当前开发暂停状态详档 |
| `CHANGELOG.md`（repo root）| 全版本变更日志 |
| `README.md`、`SECURITY.md`、`CONTRIBUTING.md` | 开源治理 |

### 1.3 数据库迁移（`migrations/` 目录）

51 个 SQL 文件，按编号顺序应用。**关键里程碑**：

| 编号 | 内容 |
|---|---|
| 001-027 | 生产 / 仓储 / 销售 / 采购 / 投入品基础表 |
| 028-038 | 采收、批次、质检、客诉、召回、期初库存导入 |
| 039-046 | 角色权限 / 内置用户 / 业务上下文 |
| 043 + 047 | CS-Core 桥接表（Chatwoot 集成）|
| 048-049 | CS 模块扩展（删除权限 + CSAT）|
| **050** | **ORG 模型**（5 表 + Albert's Meadows 树种子）|
| **051** | **Workflow 引擎**（5 表 + 3 builtin definitions）|

每个迁移有对应的 `*_rollback.sql`（部分）。

### 1.4 部署配置

| 文件 | 用途 |
|---|---|
| `backend/docker-compose.yml` | 全栈编排：mysql + redis + minio + backend + chatwoot |
| `backend/Dockerfile` | 后端镜像（multi-stage Maven 构建）|
| `backend/src/main/resources/application-dev.yml` | 开发期配置 |
| `.env.example` | 环境变量模板（SMTP / Chatwoot Token / Africa's Talking / OpenAI / Claude / DataScope）|

### 1.5 验证脚本（`/` 根目录）

| 脚本 | 用途 |
|---|---|
| `verify-v3.4.0.ps1` | CS Analytics + CSAT + 删除权限 + 周报 |
| `verify-sprint51.ps1` | ORG 模型 + DataScope |

---

## 2. 可直接复用的功能模块清单

按"开箱即用程度"分级：

### 2.1 **A 级 — 完全可复用，零改造**

| 模块 | 代码包 | 价值 |
|---|---|---|
| **客户服务模块**（Chatwoot 桥 + 多渠道 + SLA + CSAT + 周报）| `service/`、`framework/security/JwtAuthFilter` | 任何 SaaS 都需要客服，这块全套现成 |
| **组织树 + 标签**（PRD-ORG-v0.2 单树 + 标签）| `org/` | 多农场/多部门基础，VPC + SAAS 都需要 |
| **DataScope 拦截器**（MyBatis-Plus）| `framework/datascope/` | SAAS 多租户隔离时**直接拿来加 `tenant_id` 字段**即可工作 |
| **工作流引擎核心**（DSL + 5 张表 + 5 动作）| `workflow/` + migration 051 | 任何需要审批的 SaaS 立刻可用，DSL 已设计为可外配 |
| **JWT 瘦身 + 权限 Redis 缓存** | `framework/security/PermissionCacheService` | 已在 v3.5.0-rc2 完成，token 体积 / 性能均已优化 |
| **导入框架** | `framework/importer/` | Excel 导入通用框架，新模块挂 template 即可 |

### 2.2 **B 级 — 业务有强农业属性，参考价值高**

| 模块 | 农业特征 | 复用建议 |
|---|---|---|
| **生产管理**（plot / planting_plan / activity / harvest / batch）| 地块 / 计划 / 农事 / 采收 / 批次 | AgriCloud 直接照搬；非农业 SaaS 可参考"批次贯穿"模型 |
| **包装与库存**（packing / inventory / warehouse_*）| FEFO / 损耗 / 分级 / 调拨 | AgriCloud 直接照搬 |
| **质量与溯源**（qc_inspection / complaint / recall）| 农残 / 召回 / 追溯 | 食品 / 制药行业可复用 |
| **销售与履约**（customer / sales_order / fulfillment / payment / collection_log）| 订单锁库 / 出库 / 收款 / 短信催收 | 任何 B2B 销售 SaaS 可参考 |

### 2.3 **C 级 — 当前部分实现，可作为下一阶段起点**

| 模块 | 当前状态 |
|---|---|
| **审批流前端 UI**（嵌入式 badge + inline approve）| 后端 8 端点齐备，前端待实现 — AgriCloud 直接补 |
| **HR / 行政 / 法务模块** | PRD 已完成（PRD-HR-ADMIN-LEGAL-v0.2），代码 0% |
| **Finance & P&L 引擎** | 仅 cost / revenue 起步，需大量开发 |
| **AI Farm Brain** | 仅客服 AI 接通了 Claude/OpenAI，分析助手未做 |

---

## 3. AgriCloud-VPC 部署建议（单租户私有云）

### 3.1 适用场景

- 单个企业客户购买，独享一套实例
- 客户提供 VPC 网络（AWS / Azure / GCP / 本地机房）
- 不与其他客户共享数据库 / Redis / MinIO

### 3.2 改造工作量

| 项 | 工时 | 难度 |
|---|---|---|
| docker-compose 改 SaaS-grade（添加 nginx + TLS + 备份 cron）| 1d | 易 |
| 品牌替换（logo / 站点名 / favicon / 邮件模板）| 0.5d | 易 |
| 环境变量分离（`.env.vpc`）+ secrets manager 接入 | 1d | 易 |
| 多语言额外补全（PRD V2.0 提到的 Kiswahili + 法语） | 1d | 易 |
| 备份恢复方案文档化 | 0.5d | 易 |
| 监控对接（Prometheus / Grafana 已有 `docker-compose.monitoring.yml`）| 0.5d | 易 |
| **合计**: **4.5 天** | | |

### 3.3 部署清单（最小可行）

```bash
# 1. clone repo at v3.5.0-rc2
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS
git checkout v3.5.0-rc2

# 2. 配置环境变量
cp .env.example .env
# 编辑 DB password / SMTP / Chatwoot / etc

# 3. 起容器
docker compose -f backend/docker-compose.yml up -d

# 4. 等所有 healthy
docker compose -f backend/docker-compose.yml ps

# 5. 自动应用所有 migrations 001-051（v3.1.0 起 docker-compose 自动 apply）

# 6. 首次登录
# admin / Admin@123456 (首次必改密)
```

---

## 4. AgriCloud-SAAS 多租户改造路径

### 4.1 必须改造的 3 件事

**(1) tenant_id 贯穿全表**

为所有业务表加 `tenant_id BIGINT NOT NULL`，索引第一位。等同于 Sprint 51 给业务表加 `node_id` 那次迁移的模式 — `migration 050_org_model.sql` 是直接可参考的模板。

**(2) 复用 DataScope 拦截器做租户隔离**

当前 `DataScopeInnerInterceptor` 已实现"按节点子树过滤"。把它扩展成"按 tenant_id 过滤"几乎零改动：

```java
// 现有：
@DataScope(table = "plot", column = "node_id")

// 改成：
@DataScope(table = "plot", column = "node_id", tenantColumn = "tenant_id")
```

JwtAuthFilter 从 token 拿 `tid` claim，写进 ThreadLocal，拦截器自动 `WHERE tenant_id = ?` 注入。**Sprint 51 的 DataScope 子系统是这次改造最高复用资产**。

**(3) Tenant 自助 onboard 流程**

新增 `tenant` 表 + 注册 API + 试用期管理 + 计费 plan 表 + Stripe / M-Pesa 计费接入。这是 SAAS 特有的，约 **3-4 周新增工作**。

### 4.2 改造工作量

| 项 | 工时 |
|---|---|
| migration 100: 加 tenant + tenant_id 字段 + backfill default tenant | 3d |
| DataScope 拦截器加 tenant filter | 2d |
| 自助注册 + 试用期 + 计费 | 3 周 |
| 邮件 / 短信通知模板按租户隔离 | 1d |
| 跨租户管理后台（Anthropic-style "admin" 视角）| 1 周 |
| 限流 + 配额（每租户 conversation 上限等）| 3d |
| 数据导出 / 销户合规（GDPR / 肯尼亚 DPA）| 1 周 |
| **合计**: **8-10 周** | |

### 4.3 SAAS 架构建议（重点参考点）

- **数据库**：单库 + 行级 tenant 隔离（轻量，适合 < 1000 租户）；超过后切分库分租户
- **Redis**：单实例 + key prefix `tenant:{id}:*`
- **MinIO**：每租户独立 bucket
- **后端**：单一 Spring Boot 部署，所有租户共享
- **前端**：基于 vite build → CDN，按域名/子域名识别租户
- **Chatwoot**：每租户 1 个 Chatwoot account（已有 multi-account 设计）

---

## 5. 关键设计决策日志（避免后人重新踩坑）

### 5.1 ORG 模型 8 决策（详见 `PRD-ORG-v0.2.md § 0`）

1. 节点名英文单值（不做 i18n 表）
2. manager_id 单人主负责，副手用 org_user.is_manager=1
3. 物理节点（FARM/PACKHOUSE/PROCESSING/WAREHOUSE）永禁删
4. 跨场调动工资按 `effective_from/to` 按天硬切
5. data_scope='all' 角色必须落 `data_access_audit`
6. 节点类型 8 种枚举（含 PROCESSING 加工厂）
7. 接受零侵入迁移风险（ADD COLUMN NULL）
8. CTO 独立决策（无外部 review gate）

### 5.2 战略 8 决策（封版当日）

1. v3.5.0 走 HR/Admin/Legal 优先（非 Finance + AI 优先）
2. AI Provider 混合（Claude 分析 + OpenAI 客服）
3. AI Brain 推 v3.6.0
4. 90 天指标 HR 7 项
5. Partner Network 推 v4.0.0
6. Workflow 引擎核心 v3.5.0-rc2 内完成（UI 推 Sprint 53）
7. JWT 瘦身 hotfix 跟 Sprint 52 一起 commit
8. v3.5.0-rc2 作为暂停点

---

## 6. 已知限制与技术债

| # | 项 | 严重性 | 何时处理 |
|---|---|---|---|
| 1 | DataScope 默认 disabled（`agrios.datascope.enabled=false`）| 中 | SaaS 启用时必开 |
| 2 | `wf_audit` DB 触发器只防应用层，DBA SUPER 权限能绕过 | 低 | Sprint 60 移除 SUPER |
| 3 | OrgUser primary 唯一性仅 service 层校验，无 DB 约束 | 低 | 并发风险，加 unique partial index |
| 4 | Workflow 条件 grammar 极简（仅 `amount > N`）| 低 | HR Sprint 时升级到 JEXL |
| 5 | JWT 旧 token 仍带 perms claim 兼容路径在 JwtAuthFilter 里 | 低 | 2h 后自然过期，可删 |
| 6 | `data_access_audit` 无归档机制 | 低 | Sprint 60 |
| 7 | 无多语言节点名（i18n 隐含限制） | 低 | 多国扩展前补 |
| 8 | 没有 tenant_id（单租户假设）| **高 / SAAS 阻塞** | SAAS 上线前必做 |

---

## 7. 转交建议的下一步

### 立刻（接收当天）

- [ ] AgriCloud 开发团队 clone `v3.5.0-rc2` tag，跑 verify-sprint51.ps1 确认环境一致
- [ ] 读 PRD-ORG-v0.2 + PRD-HR-ADMIN-LEGAL-v0.2 + STATE-AT-PAUSE-2026-06-05.md
- [ ] 决定走 **VPC 优先** 还是 **SAAS 优先**
- [ ] 决定是否继续 HR / Admin / Legal 路线（Sprint 53-61）还是改走 Finance + AI

### 1 周内

- [ ] AgriCloud 团队产出 **`AgriCloud-Roadmap-v0.1.md`** 阐明产品差异化定位
- [ ] 提交品牌差异化方案（logo / 域名 / 邮件模板）
- [ ] 提交基础设施评估（VPC 用 AWS / Azure / GCP？）

### 1 月内

- [ ] 完成第一个 VPC 客户部署试点
- [ ] 启动 SAAS tenant_id 迁移（如果走 SAAS 路线）

---

## 8. 联络与衔接

- **源仓库**：https://github.com/2AfricaAI/2Africa-AgriOS
- **当前状态文档**：`docs/STATE-AT-PAUSE-2026-06-05.md`
- **顶层 PRD**：`docs/PRD-AGRIOS-V2.0.docx`（external）
- **客服模块设计**：`docs/2Africa-ServiceOS-V1.0-PRD.md`（如有）

接收方在开始任何重大改造之前，建议先：
1. 跑通 v3.5.0-rc2 的 docker-compose 全栈
2. 跑通 verify-sprint51.ps1 确认 ORG 模型工作正常
3. 阅读 ORG decisions + 战略 decisions（§ 5）以避免重新拍板已经定了的事

---

**v3.5.0-rc2 为正式转交点**。Source code, schema, docs, PRDs 全部齐备。AgriCloud 可独立演进，AgriOS 暂停期间不锁主分支，必要时仍可接受 PR。

— 转交完毕。
