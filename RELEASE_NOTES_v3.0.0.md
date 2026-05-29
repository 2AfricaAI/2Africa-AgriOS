# AgriOS v3.0.0 — Open Source Public Release

**Released: 2026-05-29**
**License: Apache 2.0**
**Container images:** `ghcr.io/2africaai/agrios-backend:v3.0.0`, `ghcr.io/2africaai/agrios-frontend:v3.0.0`

This is the first public open-source release of 2Africa AgriOS — a single-farm operating system designed for African agriculture, with a strict offline-first philosophy and a clean separation between the open-source farm OS (this project) and the proprietary multi-tenant 2Africa AgriCloud platform (separate product).

---

## Highlights

### Open source, Apache 2.0
- Full source available on GitHub
- Free to use, modify, redistribute, including for commercial purposes
- Explicit patent grant from contributors via the Apache 2.0 license
- DCO-based contribution model (no CLA required)

### Offline-first by design
- Every core business operation works without internet
- Network features (M-Pesa, SMS, AgriCloud federation) are optional enhancements that gracefully degrade when offline
- Mobile PWA includes an IndexedDB-backed offline queue for activity and harvest recording (Sprint 20.7)

### Mature 11-module ERP
Built up over 37 sprints since project start in Q3 2025:

- Production (plots, planting plans, activities, harvests, batches with FEFO splitting)
- Packhouse (FEFO inventory, shelf-life tracking, expiry-date enforcement)
- Sales (customers with credit terms, orders, payments, AR aging, statement PDFs)
- Procurement (suppliers, POs, AP aging, vendor payments)
- Finance (P&L by plot/SKU/customer, cash flow forecast, monthly close)
- Quality (QC inspections, PHI blocking, public traceability QR pages, complaints, recalls, GAP report exports)
- Warehouse (inbound, outbound, stocktake, transfer, scrap with full audit log)
- People & RBAC (JWT, fine-grained 11×3 permission matrix, partner & customer portals)
- Mobile PWA (English / 中文 / Swahili, GPS + photo capture, offline queue)
- Action Board (rule-driven items: overdue AR, FEFO near-expiry, low stock, etc.)
- Trace (public QR batch traceability for buyers and consumers)

### Sprint 38 — Excel bulk import (new in v3.0.0)
- Generic reusable framework (`ImportTemplate` interface + `ImportRunner` + `ImportDialog` component)
- Templates implemented for Crops, Plots, Customers, and opening Inventory balances
- Drag-and-drop upload with progress, per-row error reporting, downloadable templates

---

## What's NOT yet in v3.0.0 (roadmap for v3.1+)

| Feature | Target |
| --- | --- |
| Real M-Pesa Daraja integration (currently stubbed) | v3.1 |
| Real Africa's Talking SMS integration (currently stubbed) | v3.1 |
| Onboarding wizard for new-farm setup | v3.2 |
| Kenya demo-data one-click loader | v3.2 |
| Worker mobile v2 (my-today / week / month views) | v3.3 |
| OpenAPI client for AgriCloud federation | v3.4 |
| Plugin framework | v3.5 |
| AI vision for crop disease detection | v4.0 |

---

## Installation

### Quick start (local dev)

```bash
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS/backend
docker compose up -d --build
cd ../frontend && npm install && npm run dev
```

Open http://localhost:5173 with `admin` / `admin123`.

### Production deployment

Use the prebuilt images from GHCR:

```yaml
services:
  backend:
    image: ghcr.io/2africaai/agrios-backend:v3.0.0
  frontend:
    image: ghcr.io/2africaai/agrios-frontend:v3.0.0
```

See [DEPLOY.md](DEPLOY.md) for the full production runbook.

---

## Upgrade notes

This is a first public release, so there's nothing to upgrade from. If you have been tracking the project from an internal pre-release, see [CHANGELOG.md](CHANGELOG.md) for the full history.

---

## Known issues

- Some legacy Chinese log messages remain in code (Phase 2 cleanup planned for v3.1)
- M-Pesa integration is a stub (real integration in v3.1)
- SMS gateway is a stub (real Africa's Talking in v3.1)
- Excel import currently covers 4 tables (Crops / Plots / Customers / opening Inventory); remaining master tables planned post-v3.0 based on user demand

---

## Compatibility

- **Java:** 17 (Eclipse Temurin recommended)
- **Node:** 20+
- **MySQL:** 8.0+
- **Redis:** 7.0+
- **Browsers:** modern evergreen (Chrome, Firefox, Safari, Edge)
- **Mobile:** PWA install from any modern mobile browser

---

## Community

- GitHub: https://github.com/2AfricaAI/2Africa-AgriOS
- Issues: https://github.com/2AfricaAI/2Africa-AgriOS/issues
- Discussions: https://github.com/2AfricaAI/2Africa-AgriOS/discussions
- Email: `community@2africa.ai`

---

## Acknowledgments

To everyone who pushed this from "internal pilot" to "public open-source release" through 37 sprints of shipped work. To the local-first software movement (Ink & Switch) for naming what we were already trying to do. To the Apache Software Foundation for the license that lets us share this freely.

— 2Africa AI team
