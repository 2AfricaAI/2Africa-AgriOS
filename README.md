<div align="center">

# 🌾 2Africa AgriOS

**为非洲中小农场打造的智能农场运营 OS**
*An AI-driven Farm Operations OS for African small/medium farms*

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](#)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](#)
[![License](https://img.shields.io/badge/license-Proprietary-blue)](#license)

种植 · 采收 · 销售 · 财务 · 采购 · 决策中心 — 一个完整的农场运营闭环

</div>

---

## ✨ 亮点

- 🌍 **本地化** — KES 多币种 · Loop (NCBA) 收单 · Africa's Talking SMS
- 🧠 **决策中心** — 11 条规则自动产出今日必做 / 本周风险 / 跟进清单
- 💰 **真实成本** — PO → Activity → 5 维 P&L (Plan/Plot/SKU/Customer/Channel)
- 📊 **13 周现金流预测** — AR + AP + 客户承诺 → 资金缺口红色预警
- 📧 **AR 催收闭环** — 跟催记录 + 客户对账单 PDF + SMS 模板
- 🎨 **绿色品牌 + 中英双语** — Element Plus · vue-i18n

## 🚀 30 秒启动

```bash
# 1. 克隆
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS

# 2. 启动后端 (MySQL + Redis + MinIO + Spring Boot)
cd backend && docker compose up -d

# 3. 启动前端 dev server
cd ../frontend && npm install && npm run dev
```

访问 http://localhost:5173 — 默认账号 `admin` / `Admin@123456`

<details>
<summary>📌 其他入口</summary>

- Swagger API:    http://localhost:8080/api/swagger-ui.html
- MinIO Console:  http://localhost:9001  (minioadmin / minioadmin)
- 灌入演示数据:   `Get-Content "..\demo-data.sql" -Encoding UTF8 -Raw | docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot123456 toafrica_agrios`

</details>

## 🛠 技术栈

|        | 选型 |
| ------ | ---- |
| 后端   | Spring Boot 3.2.5 · Java 17 · MyBatis-Plus 3.5.5 · Spring Security · JWT |
| 前端   | Vue 3 · Vite · Pinia · Element Plus · vue-i18n · ECharts |
| 存储   | MySQL 8 · Redis 7 (Redisson) · MinIO (S3 兼容) |
| PDF    | OpenHtmlToPdf 1.0.10 |
| 部署   | Docker Compose |

## 🎯 业务模块

| Phase | 模块 | 状态 |
| :---: | --- | :---: |
| 1 | 主数据 (Crop/Variety/Spec/Warehouse/Plot) | ✅ |
| 1 | 生产 (Plan/Activity/Harvest/Batch + 拆分) | ✅ |
| 1 | Packhouse (SKU/Packing/Inventory) | ✅ |
| 2 | 销售 (Customer/Order/Picking/Outbound/Revenue) | ✅ |
| 2 | 财务 (Payment/AR/月度报表/Loop) | ✅ |
| 2 | P&L 5 维报表 (Plan/Plot/SKU/Customer/Channel) | ✅ |
| 2 | AR 催收 (跟催 + 对账单 PDF + SMS) | ✅ |
| 3 | 采购 (Supplier/PO/VendorPayment/AP) | ✅ |
| 3 | Activity↔PO 关联 (成本真实化) | ✅ |
| 3 | 13 周滚动现金流预测 | ✅ |
| 3 | 决策中心 11 规则 (9 真实 + 2 待品控模块) | ✅ |
| 4 | PWA 移动端 / PDF 闭环 / PHI / QC | ⏳ |

## 🧠 决策中心规则

<details>
<summary>展开 11 条规则清单</summary>

| 编号 | 名称 | 触发条件 | severity |
| :---: | --- | --- | :---: |
| R-INV-01 | 临期库存 | 库存距过期 < 阈值 | high/med |
| R-INV-02 | 高损耗 | harvest vs batch 差 > 20% | low/med/high |
| R-PROD-02 | 库存过高 | days_supply > 阈值 | medium |
| R-PROD-03 | 临采预警 | harvest_date 在未来 7 天 | medium/high |
| R-PROD-04 | 逾期未采收 | plan_harvest_date < today | medium/high |
| R-CUST-01 | 沉默客户 | > 30 天无下单 | low |
| R-AR-01 | 应收逾期 | 订单 due_date 已过 | low/med/high |
| R-AR-02 | 大额应收 | 单客户 > 200K KES | low/med/high |
| R-AP-01 | 应付逾期 | PO due_date 已过 | low/med/high |
| R-AP-02 | 大额应付 | 单供应商 > 200K KES | low/med/high |
| R-CASH-01 | 现金缺口 | 13 周累计净流 < 0 | low/med/high |

</details>

## 📁 项目结构

```
2Africa-AgriOS/
├── backend/         Spring Boot + MyBatis-Plus
│   └── src/main/java/ai/toafrica/agrios/
│       ├── master/ production/ packhouse/   生产链路
│       ├── sales/ finance/ procurement/     销售 + 财务 + 采购
│       ├── ops/                             决策中心 11 规则
│       └── framework/ system/ common/       基础设施
├── frontend/        Vue 3 + Vite + Element Plus
│   └── src/{api,views,components,layouts,i18n,router,stores}
├── migrations/      MySQL 增量迁移 SQL
├── schema.sql       初始化 schema + 种子数据
└── demo-data.sql    演示数据 (5 个肯尼亚地块)
```

## 🌐 本地化

- **本位币** KES (Kenyan Shilling) · **可选** USD / EUR + fx_rate
- **支付方式** cash · bank · cheque · **loop_online** · **loop_pos** (NCBA Loop 统一收单)
- **SMS** 抽象 `SmsProvider`,默认 stub,预留 Africa's Talking 接入

## 📷 截图

<!--
待补 — 跑起来后截图填入：
- Dashboard 首页 (KPI + 7 天趋势)
- 现金流预测 (ECharts 双轴图 + 缺口预警) ⭐
- 决策中心行动清单 (4 Tab × 11 规则)
- 销售订单 + 应付日预览
- AP 应付台账
- 客户对账单 PDF
-->

## License

Proprietary — © 2026 2Africa.AI. All rights reserved.

---

<div align="center">

由 [@2AfricaAI](https://github.com/2AfricaAI) 与 Claude (Anthropic) 协作开发
原名 *Albert's Farm* (2026-03 起步),2026-05 更名为 *2Africa AgriOS*

</div>
