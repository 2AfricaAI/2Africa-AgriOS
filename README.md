# 2Africa AgriOS

> AI 驱动的非洲（重点肯尼亚）中小农场运营操作系统。
> 覆盖种植 → 采收 → 销售 → 财务 → 决策的完整业务闭环。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](#)
[![Vue 3](https://img.shields.io/badge/Vue-3-brightgreen)](#)
[![Java 17](https://img.shields.io/badge/Java-17-orange)](#)
[![MySQL 8](https://img.shields.io/badge/MySQL-8-blue)](#)

## 核心模块

- **生产**：种植计划 / 农事 / 采收 / 批次（支持拆分）
- **打包**：SKU 自动建表 / Packing 事务核心 / 多维库存
- **销售**：客户（账期）/ 订单 / 拣货 FIFO / 出库
- **财务**：5 种付款 / AR 账龄 / 5 维 P&L / 13 周现金流 / 月报
- **采购**：供应商 / PO / AP 账龄 / Activity↔PO 真实成本
- **决策中心**：11 条规则引擎自动生成 4 类行动清单
- **移动端**：PWA H5（记农事 / 记采收 / 任务）+ GPS + 拍照

## 技术栈

后端 Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security JWT
前端 Vue 3 + Vite + Pinia + Element Plus + ECharts + i18n
存储 MySQL 8 + Redis 7 + MinIO；PDF OpenHtmlToPdf；部署 Docker Compose

## 快速启动

```bash
# 后端
cd backend && docker compose up -d
# 前端
cd frontend && npm install && npm run dev
# 默认账号 admin / Admin@123456
```

访问：前端 http://localhost:5173 · 移动端 http://localhost:5173/m/ · Swagger http://localhost:8080/api/swagger-ui.html

## License

Proprietary © 2026 2Africa.AI. 由 [@2AfricaAI](https://github.com/2AfricaAI) 与 Claude (Anthropic) 协作开发。
