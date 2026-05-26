# 2Africa AgriOS

> An AI-driven Farm Operations OS for African small/medium farms.
> 为非洲中小农场打造的智能农场运营系统。

[![License](https://img.shields.io/badge/license-Proprietary-blue.svg)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](#)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](#)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](#)

---

## 📌 项目简介

**2Africa AgriOS** 是一个为非洲（重点肯尼亚）中小农场设计的**全栈农场运营操作系统**，覆盖从田头种植 → 采收打包 → 销售收款 → 采购付款 → 财务报表 → 决策中心的**完整业务闭环**。

**核心特色：**

- 🌍 **本地化优先**：KES 多币种 + Loop (NCBA) 收单 + Africa's Talking SMS 框架
- 🧠 **决策中心驱动**：11 条规则引擎自动生成今日必做 / 本周风险 / 跟进 / 暂停 4 大行动清单
- 💰 **真实成本归集**：采购单据 → Activity → Plan/Batch/SKU/Plot/Customer/Channel 5 维 P&L 报表
- 📊 **13 周滚动现金流预测**：消化 AR + AP + 承诺还款，提前预警资金缺口
- 📧 **AR 催收闭环**：跟催记录 + 客户对账单 PDF + SMS 模板
- 🎨 **绿色品牌 + 中英双语**：Element Plus + i18n 全面覆盖

---

## 🎯 业务能力总览

```
Phase 1  基础数据 + 生产追溯
─────────────────────────────────────
✅ 主数据  作物 / 品种 / 包装规格 / 仓库库位 / 地块
✅ 生产    种植计划 / 农事记录 (含照片) / 采收记录 / 批次追溯 (支持拆分)
✅ 打包    SKU 自动建表 / Packing 事务核心 / Inventory 多维查询

Phase 2  销售 + 财务 + 决策
─────────────────────────────────────
✅ 销售    客户 (含账期) / 销售订单 / 拣货 (FIFO) / 出库 / Revenue 流水
✅ 财务    付款 (5 种 method) / AR 账龄 / 月度报表 / Loop 收单
✅ P&L     Plan / Plot / SKU / Customer / Channel 5 维报表
✅ 决策中心 11 条规则 (临期/沉默/逾期/缺口/集中度/临采…)

Phase 3  采购 + 应付 + 现金流 + 移动 (规划)
─────────────────────────────────────
✅ 采购    供应商 / 采购订单 / 收货 / VendorPayment / AP 账龄
✅ Activity 关联 PO → 成本数据真实化
✅ 13 周滚动现金流预测 + 资金缺口预警
✅ AR 催收闭环 (跟催记录 + 对账单 PDF + SMS 框架)
⏳ PWA 农场工人移动端 (规划中)
⏳ PHI 喷药安全期 + QC 投诉模块 (规划中)
```

---

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.5 + Spring Security + JWT |
| 前端 | Vue 3 + Vite + Pinia + Element Plus + vue-i18n + ECharts |
| 数据库 | MySQL 8 (utf8mb4) |
| 缓存 | Redis 7 + Redisson |
| 存储 | MinIO (S3 兼容，预签名 URL) |
| PDF | OpenHtmlToPdf 1.0.10 (HTML → PDF) |
| 部署 | Docker Compose (MySQL / Redis / MinIO / Backend) |

---

## 🚀 快速启动

### 前置要求

- Docker Desktop（包含 docker compose）
- Node.js 18+
- Java 17（仅本地开发，容器化部署不需要）

### 1. 克隆 + 初始化

```bash
git clone https://github.com/wmxeisoo2008-eng/2Africa-AgriOS.git
cd 2Africa-AgriOS
```

### 2. 启动后端（MySQL + Redis + MinIO + Spring Boot）

```bash
cd backend
docker compose up -d
docker compose logs -f backend --tail 30
```

等到 `Started AgriOsApplication` 出现就可以了。

访问：
- Swagger: http://localhost:8080/api/swagger-ui.html
- MinIO Console: http://localhost:9001 （默认 minioadmin / minioadmin）

### 3. 启动前端 dev server

```bash
cd frontend
npm install
npm run dev
```

打开浏览器：http://localhost:5173

默认账号：`admin` / `Admin@123456`

### 4. (可选) 灌入演示数据

```bash
cd backend
Get-Content "..\demo-data.sql" -Encoding UTF8 -Raw | docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot123456 toafrica_agrios
```

---

## 📁 项目结构

```
2Africa-AgriOS/
├── backend/                          Spring Boot 后端
│   ├── docker-compose.yml            (MySQL/Redis/MinIO/Backend)
│   ├── Dockerfile                    多阶段 Maven 构建
│   ├── pom.xml
│   └── src/main/java/ai/toafrica/agrios/
│       ├── common/                    R / PageQuery / 全局异常
│       ├── framework/                 JWT / Security / Jackson / MinIO 配置
│       ├── system/                    Auth / 用户 / 文件
│       ├── master/                    主数据 (Crop/Variety/Spec/Warehouse)
│       ├── production/                生产 (Plot/Plan/Activity/Harvest/Batch)
│       ├── packhouse/                 打包 (SKU/Packing/Inventory)
│       ├── sales/                     销售 (Customer/Order/Fulfillment)
│       ├── finance/                   财务 (Payment/AR/PnL/CashFlow/Statement)
│       ├── procurement/               采购 (Supplier/PO/VendorPayment/AP) ⭐
│       └── ops/                       决策中心 (ActionRule × 11)
│
├── frontend/                         Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/                       8+ 业务 API 模块
│       ├── components/                FileUploader / PaymentDialog / ...
│       ├── views/                     12+ 业务页面 (master/production/packhouse/sales/finance/procurement/operations)
│       ├── layouts/AppLayout.vue      侧栏 + 顶栏 + 语言切换
│       ├── router/                    Vue Router (含权限守卫)
│       ├── stores/                    Pinia (auth)
│       ├── i18n/                      en + zh 字典 (1000+ key)
│       └── utils/request.js           axios 拦截器 (JWT + Blob)
│
├── migrations/                       MySQL 增量迁移 (009 ~ 020)
├── schema.sql                        初始化 schema (含种子用户/角色/字典)
├── demo-data.sql                     演示数据 (5 个肯尼亚地块 / 作物 / 计划)
└── 2Africa-AgriOS-产品规划V2.0.docx   完整产品规划 (Phase 1-3)
```

---

## 🌟 核心功能截图

> 截图待补 — 建议先跑起来后截图填上：
> - Dashboard 首页 (KPI + 7 天趋势)
> - 农事记录 (生产追溯)
> - 销售订单 + 应付日预览
> - P&L 报表 (5 维)
> - **现金流预测** (蓝色 hero + ECharts 双轴图 + 资金缺口预警) ⭐
> - **决策中心行动清单** (4 Tab × 11 规则)
> - 采购订单 + AP 应付台账
> - 客户对账单 PDF

---

## 🧠 决策中心规则清单

| 编号 | 名称 | 触发条件 | severity |
|---|---|---|---|
| R-INV-01 | 临期库存 | 库存批次距过期 < 阈值 | high/med |
| R-INV-02 | 高损耗 | harvest vs batch 差值 > 20% | low/med/high |
| R-PROD-02 | 库存过高 | days_supply > 阈值 | medium |
| R-PROD-03 | 临采预警 | plan_harvest_date 在未来 7 天内 | medium/high |
| R-PROD-04 | 逾期未采收 | plan_harvest_date < today | medium/high |
| R-CUST-01 | 沉默客户 | 客户 > 30 天无下单 | low |
| R-AR-01 | 应收逾期 | 订单 due_date 已过 | low/med/high |
| R-AR-02 | 大额应收 | 单一客户 outstanding > 200K KES | low/med/high |
| R-AP-01 | 应付逾期 | PO due_date 已过 | low/med/high |
| R-AP-02 | 大额应付 | 单一供应商 payable > 200K KES | low/med/high |
| R-CASH-01 | 现金缺口 | 未来 13 周累计净流 < 0 | low/med/high |

---

## 📦 Docker 容器布局

| 容器 | 镜像 | 端口 | 用途 |
|---|---|---|---|
| toafrica-mysql | mysql:8.0 | 3306 | 业务数据 |
| toafrica-redis | redis:7-alpine | 6379 | 缓存 / Redisson 分布式锁 |
| toafrica-minio | minio/minio:latest | 9000/9001 | S3 兼容对象存储 |
| toafrica-backend | agrios-backend:dev | 8080 | Spring Boot 应用 |

---

## 🌐 多币种 + 本地化

- **本位币**：KES (Kenyan Shilling)
- **可选币种**：USD / EUR (订单/PO 录入时可选 + fx_rate)
- **支付方式**：cash / bank / cheque / **loop_online** / **loop_pos** (NCBA Loop 统一收单)
- **SMS 框架**：抽象 `SmsProvider` 接口，默认 stub，预留 Africa's Talking 接入

---

## 🗺️ Sprint 路线图

| Phase | Sprint | 主题 | 状态 |
|---|---|---|---|
| 1 | 1-7 | 基础数据 + 生产追溯 + 打包库存 + Dashboard | ✅ |
| 2 | 8-9 | 品牌 + i18n + 销售链全链路 | ✅ |
| 2 | 10 | 决策中心 (3 实 + 6 stub 规则) | ✅ |
| 2 | 11-13 | InputCost + 成本归集 + P&L 报表 | ✅ |
| 2 | 14-15 | Payment + AR + 月度报表 + Loop | ✅ |
| 2 | 16 | AR 催收闭环 + 对账单 PDF + SMS | ✅ |
| 3 | 17 | 采购 + 供应商 + AP + Activity↔PO | ✅ |
| 3 | 18 | 13 周现金流预测 + R-CASH-01 | ✅ |
| 3 | 19 | 决策中心规则全线真实化 (9/11) | ✅ |
| 3 | 20+ | PWA 移动端 / PDF 闭环 / PHI / QC | ⏳ |

---

## 📄 License

Proprietary — © 2026 2Africa.AI. All rights reserved.

---

## 🙏 关于

由 [@wmxeisoo2008-eng](https://github.com/wmxeisoo2008-eng) 与 Claude (Anthropic) 协作开发。
项目从 2026 年 3 月起步，原名 "Albert's Farm"，2026 年 5 月更名为 "2Africa AgriOS"。
