# Changelog

All notable changes to AgriOS are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Roadmap targets:
- v3.2: M-Pesa Daraja real integration, Africa's Talking SMS integration
- v3.3: Onboarding wizard, Kenya demo data one-click load
- v3.4: Worker mobile v2 (my today / week / month views)
- v3.5: OpenAPI client (AgriOS to AgriCloud federation)

## [3.1.0] - 2026-06-01

Customer Service module — verified release. Same scope as 3.1.0-rc1 plus
one infrastructure fix below; no AgriOS application code changes.

### Fixed (Sprint 40h)

- **Chatwoot worker restart loop**. The original `docker-compose.yml`
  overrode Chatwoot's built-in entrypoint with `entrypoint: sh -c`,
  bypassing the wait-for-postgres / wait-for-redis / bundle-setup logic
  that ships in the upstream image. The worker container then crashed in
  a tight restart loop on first boot. Fix:
  - Removed `entrypoint: sh -c` override on `chatwoot-web` and
    `chatwoot-worker`. Upstream entrypoint now handles dependency waits.
  - Added a one-shot `chatwoot-db-prepare` service that runs
    `rails db:chatwoot_prepare` and exits. Web and worker depend on its
    successful completion, so they never race the schema migration.
  - Loosened the `chatwoot-web` healthcheck so a 302 redirect from `/`
    no longer marks the container unhealthy.

### Verified on a fresh stack (Sprint 40h–40k)

- 8 containers all `healthy` / `Up` after `docker compose up -d`
- AgriOS backend `/v1/service/health` reports `enabled=true, reachable=true`
  with the new Chatwoot API token
- AI Agent diagnostic endpoint
  (`POST /v1/service/ai-agent/diagnose`) returns a non-empty OpenAI
  `gpt-4o-mini` reply in ~3 seconds — provider abstraction works
  end-to-end

### Deferred to v3.1.1

- Email Inbox setup with Google Workspace (runtime configuration, not a
  code change; will follow once the Workspace account is provisioned)
- Real-world end-to-end test (external email → Chatwoot → AI reply)

## [3.1.0-rc1] - 2026-05-31

Customer Service module via Chatwoot integration. Sprint 40.

### Added — Service module (Sprint 40a–40f)

- **AgriOS ↔ Chatwoot bridge** — embedded MIT-licensed Chatwoot stack
  alongside AgriOS in `backend/docker-compose.yml` (web + worker + dedicated
  PostgreSQL with pgvector + dedicated Redis). Decision recorded:
  Mode 1 "Embedded" (Chatwoot ships with each AgriOS install,
  offline-friendly). Future Mode 2 "Hosted" deferred to 2Africa ServiceOS.
- **Two minimal bridge tables**, not a copy of Chatwoot's schema:
  - `service_contact_link` — maps AgriOS Customer (and future vertical
    entities like supplier / worker / partner) to Chatwoot Contact
  - `service_event_log` — cross-system event audit trail with idempotency
    keys for duplicate-webhook protection
- **ChatwootClient** (Java) — REST API client: contact CRUD, conversation +
  message endpoints, profile ping. Resilient parsing across Chatwoot's
  multiple response envelope shapes.
- **ContactSyncService** — one-way push of AgriOS Customer master data to
  Chatwoot Contact. Idempotent (adopts orphan Chatwoot contacts via
  identifier lookup before creating duplicates), auto-normalizes Kenyan
  phone numbers to E.164, mirrors `agrios_customer_*` business attributes
  into Chatwoot custom attributes for in-context CSR work.
- **ChatwootWebhookController** — public endpoint at
  `/v1/service/webhook/chatwoot`. Optional HMAC-SHA256 signature
  verification. Reverse-resolves AgriOS entity from Chatwoot contact id.
- **AI Agent** — provider-pluggable LLM layer (`LlmClient` interface +
  `LlmRouter` + `ClaudeClient` + `OpenAiClient`). Switch between Anthropic
  Claude and OpenAI (or any OpenAI-compatible gateway — Azure, OpenRouter,
  Groq, Ollama, etc.) via a single env var, zero code change. Default reply
  mode is private note (human-in-the-loop); operators can flip to fully
  automated public replies. Diagnostic endpoint
  `POST /v1/service/ai-agent/diagnose` for end-to-end LLM verification
  without the Chatwoot dependency.
- **Frontend Customer Service workspace** (`/service`) — embeds Chatwoot
  via iframe with graceful "Open in new tab" fallback for browsers /
  Chatwoot installs that block iframe embedding. `FRAME_ANCESTORS`
  whitelist for AgriOS origins. New top-level sidebar menu item,
  translated en / zh / sw.

### Configuration

- New env vars: `CHATWOOT_BASE_URL`, `CHATWOOT_API_TOKEN`,
  `CHATWOOT_ACCOUNT_ID`, `CHATWOOT_WEBHOOK_SECRET`, `AI_AGENT_ENABLED`,
  `AI_AGENT_PROVIDER`, `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`,
  `OPENAI_BASE_URL`, `OPENAI_ORGANIZATION`, `AI_AGENT_MODEL`,
  `AI_AGENT_REPLY_PUBLIC`. All optional; service module is opt-in.

### Documentation

- `docs/SERVICE_MODULE.md` — full operator guide: stack startup, first-time
  login, contact sync flow, webhook configuration, Email Inbox setup
  (Gmail App Password and Microsoft 365 paths), AI Agent setup (Claude
  + OpenAI + Azure / OpenRouter / Ollama walkthroughs), troubleshooting,
  production hardening checklist.

### Notes

- WhatsApp Cloud API inbox deliberately deferred — requires Meta business
  verification with multi-week lead time. Email + AI Agent cover ~85% of
  Kenyan smallholder customer touchpoints at MVP.
- Migration `043_service_module.sql` is idempotent (`CREATE TABLE IF NOT
  EXISTS`) so it can be safely re-run.

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
