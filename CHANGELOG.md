# Changelog

All notable changes to AgriOS are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Roadmap targets for v3.1 onward:
- v3.1: M-Pesa Daraja real integration, Africa's Talking SMS integration
- v3.2: Onboarding wizard, Kenya demo data one-click load
- v3.3: Worker mobile v2 (my today / week / month views)
- v3.4: OpenAPI client (AgriOS to AgriCloud federation)

## [3.0.0] - 2026-05-29

First public open-source release under Apache License 2.0.

### Added
- **Excel bulk import framework** (Sprint 38): generic `ImportTemplate` interface with reusable `ImportRunner` and frontend `ImportDialog` component. Templates implemented for Crops, Plots, Customers, and opening Inventory balances.
- **Fine-grained RBAC** (Sprints 34-37): 11 modules x 3-tier access matrix (None / Read / Write), Stripe-style permission UI, custom roles, partner & customer user types, customer self-service portal.
- **Offline-first principle** formally adopted: all core operations work without network connectivity. Mobile PWA includes IndexedDB-backed offline queue for activity and harvest recording.

### Mature modules (from Sprints 1-33)
- Production: plots, planting plans, activities, harvests, batches with FEFO splitting
- Packhouse: FEFO inventory with shelf-life tracking and expiry-date enforcement
- Sales: customers (with credit terms), orders, payment status, AR aging, statement PDFs
- Procurement: suppliers, POs (with auto-cost on activities), AP aging, vendor payments
- Finance: P&L by plot/SKU/customer/channel, cash-flow forecast, monthly close
- Quality: QC inspections, PHI blocking, public traceability QR pages, complaints, recalls with PDF reports, GAP report exports (PDF + Excel)
- Warehouse: inbound, outbound, stocktake, transfer, scrap with full audit log
- People: JWT + RBAC, partner subtypes, customer portal at `/portal`
- Mobile PWA: English / 中文 / Swahili, GPS + photo capture, offline queue
- Action Board: rule-driven action items (overdue AR, FEFO near-expiry, high waste, etc.)

### Operations & deployment
- Production Docker images on GitHub Container Registry
- Backup scripts (`bin/backup-db.sh`, `bin/backup-minio.sh`, `bin/restore-db.sh`)
- Monitoring stack: Uptime Kuma + Spring Boot Actuator health endpoints
- Multi-environment configs (dev / prod profiles), externalised secrets

### Documentation
- LICENSE (Apache 2.0)
- NOTICE (third-party attributions)
- CONTRIBUTING.md
- CODE_OF_CONDUCT.md
- SECURITY.md
- DEPLOY.md (production deployment runbook)
- Product PRDs: AgriOS V3.0, AgriCloud V1.0, OpenAPI Spec V1.0

### Internal notes
- Licence formally chosen: Apache 2.0. Previous `LICENSE` (Unlicense) replaced. Choice rationale in AgriOS V3.0 PRD Chapter 3.
- Original product name "Albert's Farm" superseded by "2Africa AgriOS" (Java package renamed `ai.toafrica.agrios`).

[Unreleased]: https://github.com/2AfricaAI/2Africa-AgriOS/compare/v3.0.0...HEAD
[3.0.0]: https://github.com/2AfricaAI/2Africa-AgriOS/releases/tag/v3.0.0
