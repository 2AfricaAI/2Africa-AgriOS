# 2Africa AgriOS

[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-3.0.0-green.svg)](CHANGELOG.md)
[![Built for Kenya](https://img.shields.io/badge/built%20for-Kenya%20%F0%9F%87%B0%F0%9F%87%AA-orange.svg)](#)

**An open-source, offline-first farm operating system, designed for African agriculture.**

AgriOS digitizes the day-to-day operations of a single farm — from planting and harvesting to packing, sales, finance, and quality compliance. It runs locally on your own server, requires no internet to function, and never sends your data anywhere by default.

If you choose, AgriOS can federate with **2Africa AgriCloud** (a separate platform) via the **2Africa OpenAPI** to unlock network effects: peer benchmarks, market price discovery, bulk procurement, credit packs for banks, and more — all opt-in, field-by-field.

---

## Why AgriOS?

- **Your code.** Apache 2.0 licensed. Read it, modify it, ship your own variant.
- **Your data.** Everything stays on your machine. No telemetry by default.
- **Your network.** Works fully offline. Internet is an enhancement, not a requirement.
- **Your terms.** Use commercially. Integrate into proprietary systems. Charge for support.

---

## What's inside

AgriOS covers eleven business modules out of the box:

| Module | Highlights |
| --- | --- |
| **Production** | Plots, planting plans, activities, harvests with GPS + photos |
| **Packhouse** | FEFO inventory, shelf-life tracking, batch traceability |
| **Quality** | QC inspections, PHI blocking, GAP reports, complaints, recalls |
| **Sales** | Customers, orders, AR aging, customer statements (PDF) |
| **Procurement** | Suppliers, POs, AP aging, vendor payments |
| **Finance** | P&L by plot/SKU/customer, cash flow forecast, monthly close |
| **Warehouse** | Inbound, outbound, stocktake, transfers, scrap |
| **People & RBAC** | Fine-grained module × tier permissions, partner & customer portals |
| **Mobile PWA** | Offline activity & harvest recording with GPS + photos (English / 中文 / Swahili) |
| **Action Board** | Rule-driven action items: payment overdue, FEFO near-expiry, stock low, etc. |
| **Trace** | Public QR-code batch traceability page for buyers and consumers |

---

## Quick start (local development)

Prerequisites: Docker, Docker Compose, Node 20+, Java 17 (for IDE only).

```bash
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS

# Backend (MySQL + Redis + MinIO + API)
cd backend
docker compose up -d --build
docker compose logs -f backend

# Wait for "Started AgriOsApplication" then in another terminal:
cd ../frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in with `admin` / `admin123`.

API docs: http://localhost:8080/api/swagger-ui/index.html

For production deployment see [DEPLOY.md](DEPLOY.md).

---

## Architecture

```
            ┌─────────────────────┐
  Mobile ──▶│   Frontend (Vue 3)  │
            └──────────┬──────────┘
                       │
            ┌──────────▼──────────┐
            │  Backend (Spring 3) │ ── MySQL
            │  + JWT + RBAC       │ ── Redis
            │  + Rule engine      │ ── MinIO
            └──────────┬──────────┘
                       │  (optional)
            ┌──────────▼──────────┐
            │  2Africa OpenAPI    │ ── 2Africa AgriCloud
            └─────────────────────┘
```

- **Backend**: Java 17, Spring Boot 3, MyBatis-Plus, Apache POI, OpenHtmlToPdf
- **Frontend**: Vue 3, Vite, Element Plus, ECharts, Pinia, vue-i18n
- **Storage**: MySQL 8, MinIO (S3-compatible), Redis 7
- **Deployment**: Docker Compose (default), Nginx reverse proxy, GitHub Container Registry

---

## The 2Africa product family

AgriOS is part of a three-product family:

- **2Africa AgriOS** *(this repo, Apache 2.0)* — single-farm operating system
- **2Africa AgriCloud** *(proprietary SaaS)* — multi-tenant industry platform
- **2Africa OpenAPI** *(CC-BY 4.0 spec, Apache 2.0 SDKs)* — federation protocol

AgriOS runs perfectly on its own. Connecting to AgriCloud is opt-in and unlocks network features like peer benchmarks and marketplace sourcing.

---

## Contributing

We welcome bug reports, feature requests, translations, and pull requests. Please read:

- [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community standards
- [SECURITY.md](SECURITY.md) for reporting security issues

All contributions are licensed under Apache 2.0 with a DCO sign-off.

---

## License

Copyright 2026 2Africa AI and contributors

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for third-party attributions.

---

## Status

AgriOS is **production-ready** for single-farm deployment, currently piloting in Kenya. The roadmap targets v3.1 (M-Pesa real integration), v3.2 (onboarding wizard), and v3.3 (worker mobile v2) in the next quarter.

See [CHANGELOG.md](CHANGELOG.md) for release history.
