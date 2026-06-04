# Changelog

All notable changes to AgriOS are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Roadmap targets:
- v3.5: ORG model (single tree + tags) + Workflow engine + HR + Admin + Legal/Compliance
        (see `docs/PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.1.md` and `docs/PRD-ORG-v0.1.md`)
- v3.6: Worker mobile v2 (my today / week / month views)
- v3.7: OpenAPI client (AgriOS to AgriCloud federation)

## [3.4.0] - 2026-06-04

CS Analytics + CSAT + Weekly Digest + SUPER_ADMIN delete-conversation hotfix.
Covers Sprint 49.5 + Sprint 50 (a/b/c/d/e/f) + a small i18n hotfix.

### Added — CS Analytics SLA metrics (Sprint 50a, 50b)

- **First Response Time (FRT)** in `GET /v1/cs/analytics/overview`.
  Per-conversation: first inbound message (customer) to first non-private
  outbound (agent or AI bot). Aggregated as avg / P50 / P90 with nearest-rank
  percentiles; negative deltas are discarded as clock-skew defence.
- **Time-To-Resolution (TTR)**. MVP definition `lastActivityAt - createdAt`
  for resolved conversations in the window. Approximation is documented;
  proper fix needs exposing `message_type=2` activity rows on `ChatMessage`,
  deferred.
- A 5th purple KPI card (FRT) and a 6th terracotta KPI card (TTR) on the
  Analytics dashboard, with avg as the headline value and P50/P90 + sample
  size beneath. New `formatDuration()` renders `45s / 12m / 1h 5m / 2d 3h`.

### Added — Per-agent SLA leaderboard (Sprint 50c)

- New endpoint `GET /v1/cs/analytics/agents?days=30`.
- Groups windowed conversations by `assigneeId`, computes per-agent FRT +
  TTR + assigned/resolved counts, sorts by `resolvedCount desc` with the
  synthetic "Unassigned" row forced to the bottom.
- Frontend renders a sortable Element Plus table beneath the charts.
- Separate `leaderboardCache` with 5-min TTL keyed on window days.

### Added — CSAT customer satisfaction survey (Sprint 50d)

- New table `cs_csat_response` (`migrations/049_cs_csat.sql`) — 24-char
  base32 token (~117 bits entropy), TTL 30 days, single-use semantics.
- `CsatService`:
  - `generateLink(conv, user)` is idempotent within TTL (same conversation
    in TTL returns the same token, no garbage rows on repeat clicks).
  - `loadByToken` handles expiry + already-submitted with business errors.
  - `submit(token, rating, comment)` writes once, throws on second attempt.
  - `computeSummary(days)` rolls up avg rating + thumbs-up % for the dashboard.
- `CsatController`:
  - `POST /v1/cs/csat/link` (auth + `cs:csat:send` permission).
  - `GET/POST /v1/cs/csat/public/{token}` — **no JWT**, customer-facing.
- Public survey page at `/csat/:token` (route `meta.public=true`), outside
  the auth wall. 5-star picker + optional comment, three states
  (probe / ready / thanks / error), all branded with the 2Africa.AI AgriOS
  logo. `request.js` interceptor skips the `/login` bounce for public routes.
- "Send CSAT" button in `ConversationDetail.vue` (v-perm-gated) copies the
  link to clipboard for the agent to paste into their next reply.
- Dashboard gains a 7th gold KPI card showing avg rating + thumbs-up %.
- Permission `cs:csat:send` seeded to SUPER_ADMIN / MANAGER / LEADER /
  PACKHOUSE / SALES (WORKER intentionally excluded).

### Added — Weekly digest email (Sprint 50e)

- New `WeeklyDigestService` runs on Spring cron
  (`@Scheduled(cron = "${agrios.digest.cron:0 0 6 ? * MON}")` — Monday 06:00
  server time by default).
- Builds a self-contained HTML body from `overview(7) + agentLeaderboard(7)`,
  inline CSS (most email clients strip external CSS), three-locale support
  (en / zh / sw) via an inline `record I18n` (no Thymeleaf dependency).
- New endpoints:
  - `GET /v1/cs/analytics/digest/preview` returns the rendered HTML for
    eyeballing without sending.
  - `POST /v1/cs/analytics/digest/send-now` (SUPER_ADMIN) fires an immediate
    send. Useful for testing and "weekly digest, but today".
- Configuration under `agrios.digest.*` (recipients, cc, from, cron, window,
  subject prefix, locale) + standard `spring.mail.*` for SMTP. Defaults
  target Gmail submission port with STARTTLS — works with a Gmail App
  Password without any custom mail server.
- Gated on `agrios.digest.enabled=true` so a deploy without SMTP creds does
  not crash at startup; scheduled tick logs and skips when no recipients.

### Added — SUPER_ADMIN delete conversation (Sprint 49.5 hotfix)

- New permission `cs:conversation:delete`
  (`migrations/048_cs_delete_perm.sql`) bound to SUPER_ADMIN only.
- `ConversationController.delete()` with
  `@PreAuthorize("hasAuthority('cs:conversation:delete')")` forwards the
  delete to Chatwoot's REST DELETE. Audit `WARN` log line on every call.
- Frontend: red Delete button guarded by `v-perm` on the conversation
  header, two-step `ElMessageBox.confirm` with customer name interpolated.
  On success the parent list refreshes immediately (no 10s poll wait).

### Fixed — date-range picker i18n keys (hotfix)

- `date.rangeSep / date.start / date.end` were used by four production
  pages (ActivityList / BatchList / HarvestList / PlantingPlanList) but
  the keys never existed; under the `zh` locale the raw keys leaked into
  the header. Added the three keys to each of `en / zh / sw`.

### New endpoints summary

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/v1/cs/analytics/overview?days=N` | JWT | Now includes `frtMetrics`, `ttrMetrics`, `csatMetrics` |
| GET | `/v1/cs/analytics/agents?days=N` | JWT | Per-agent SLA leaderboard |
| POST | `/v1/cs/csat/link` | JWT + `cs:csat:send` | Generate / reuse survey link |
| GET | `/v1/cs/csat/public/{token}` | public | Probe survey, no JWT |
| POST | `/v1/cs/csat/public/{token}` | public | Submit rating + comment |
| DELETE | `/v1/service/conversations/{id}` | JWT + `cs:conversation:delete` | Hard delete (SUPER_ADMIN) |
| GET | `/v1/cs/analytics/digest/preview` | JWT | Preview digest HTML |
| POST | `/v1/cs/analytics/digest/send-now` | JWT + SUPER_ADMIN | Trigger digest send |

### Migrations

- `048_cs_delete_perm.sql` — `cs:conversation:delete` permission seed
- `049_cs_csat.sql` — `cs_csat_response` table + `cs:csat:send` permission seed

### Verified

- Sprint 50a/b/c: overview + leaderboard endpoints return populated
  metrics on real Chatwoot data (13 open conversations, FRT n=3, leaderboard
  with Unassigned row)
- Sprint 50d: link generation idempotent, public probe + submit work without
  JWT, second submit on same token correctly rejected, expired token
  correctly rejected
- Sprint 50e: build clean, `digest/preview` returns rendered HTML, Gmail
  STARTTLS path configurable via env
- Sprint 49.5: SUPER_ADMIN deletes 200, non-SUPER_ADMIN deletes 403, list
  view refreshes immediately on return from detail page
- i18n: zh locale no longer leaks raw `date.rangeSep` key on production pages

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
