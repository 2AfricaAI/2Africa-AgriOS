# Changelog

All notable changes to AgriOS are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Roadmap targets:
- v3.5: Workflow engine + HR + Admin + Legal/Compliance
        (see `docs/PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2.md`)
- v3.6: Worker mobile v2 (my today / week / month views)
- v3.7: OpenAPI client (AgriOS to AgriCloud federation)

## [3.5.0-rc2] - 2026-06-05

Sprint 52 backend (Workflow engine) + JWT slim-token hotfix.
**Development paused at this tag.** See `docs/STATE-AT-PAUSE-2026-06-05.md`
to resume.

### Added -- Workflow engine backend (Sprint 52 Day 1-4)

- **migration 051** — 5 new tables: `wf_definition / wf_instance / wf_step /
  wf_audit / wf_delegation`. 3 built-in templates seeded:
  - `hr.leave_request` (1 step, manager approves, 48h SLA)
  - `admin.expense` (1 step + CFO when amount > 50,000 KES)
  - `finance.payment_out` (3 tiers, Finance Manager → CFO > 100k → SUPER_ADMIN > 1M)
- DB triggers make `wf_audit` UPDATE/DELETE-proof at the database level
  (decision §0 "audit unchangeable" enforced even against DBAs).
- 5 permissions seeded: `wf:def:manage` (SUPER_ADMIN), `wf:instance:list`
  (everyone), `wf:instance:approve` (MANAGER+), `wf:instance:withdraw`
  (everyone), `wf:instance:delegate` (MANAGER+).
- **Java layer**: 5 entities, 5 mappers, `WorkflowSchema` JSON DSL with
  snake_case Jackson naming, `WorkflowSchemaParser` with validation,
  `WorkflowEngine` (submit / advance / resolveAssignee with delegation /
  evaluateCondition minimal grammar), `WorkflowAuditService` (auto IP/UA),
  `WorkflowDelegationService` (module-scoped delegation resolution),
  `WorkflowStepService` (5 actions: approve / reject / returnTo /
  delegate / withdraw, with authorization checks), `WorkflowController`
  (8 REST endpoints under `/v1/wf/*`), `WorkflowSlaScheduler`
  (`@Scheduled(cron = "0 */5 * * * *")` SLA breach detection + escalation).

### Added -- JWT slim-token hotfix

- New `PermissionCacheService` -- moves the perm string list OUT of the
  JWT body and into Redis (`auth:perms:<uid>`, 1h TTL).
- `AuthService.login` no longer writes the `perms` claim into the JWT.
- `JwtAuthFilter` reads `perms` from cache on every request, with DB
  miss-fill. Legacy tokens that still carry the `perms` claim continue
  to work until they expire (back-compat path).
- **Effect: access token size 3300 chars → 312 chars (10.6× smaller)**.
- Risk avoided: nginx / cloud LB `large_client_header_buffers` overflow
  once HR sprint adds another 30+ perms.

### Fixed -- OrgTreePage `users.map is not a function`

- `listUsers` returns `{ list, total, page, size }`, not `{ rows }`.
  `OrgTreePage.vue` was assuming the wrong key; fixed by defensive
  unwrap that tries `list / rows / records` and falls back to array.
- Page-size parameter corrected from `pageSize: 200` to `size: 200`
  (matches the `PageQuery` Spring binding).

### Deferred (Day 5 of Sprint 52) -- when HR resumes

- Frontend "my pending" badge in business module navs
- Inline approve / reject buttons on business detail pages
- `verify-sprint52.ps1` E2E script
- Workflow delegation management UI (user-facing self-service)
- SLA breach → Chatwoot / SMS notification hook

### Added -- AgriCloud handover documentation

- `docs/2AfricaAI-AgriCloud-Handover-v0.1.md` — official handover package
  to the 2Africa.AI AgriCloud product line. Covers asset inventory,
  reusability tier list (A/B/C grade), AgriCloud-VPC deployment plan
  (4.5 days, single-tenant), AgriCloud-SAAS multi-tenant refactor path
  (8-10 weeks), tech debt register, and decision log.

### Notes

- v3.5.0-rc2 is the final commit before development pause. v3.5.0 (final)
  ships once Sprint 53-61 complete the HR / Admin / Legal modules.
- All decisions and roadmap snapshot recorded in
  `docs/STATE-AT-PAUSE-2026-06-05.md`.
- AgriCloud product line receives the handover package -- see
  `docs/2AfricaAI-AgriCloud-Handover-v0.1.md`.

## [3.5.0-rc1] - 2026-06-05

Sprint 51 -- Organization tree + DataScope subsystem.
Implementation of PRD-ORG-v0.2 (all 8 decisions locked). Foundation
for HR / Admin / Legal modules in Sprint 52+.

### Added -- ORG schema (migration 050)

- 5 new tables: `org_node`, `org_tag`, `org_node_tag`, `org_user`,
  `data_access_audit`
- 8-type node enum: GROUP / FARM / PACKHOUSE / **PROCESSING** /
  WAREHOUSE / DEPT / TEAM / PROJECT (with DB CHECK constraint)
- `location` field for geo-aware compliance / tax / insurance
- `effective_from / effective_to` on `org_user` -- foundation for
  cross-farm payroll attribution (decision #4)
- `node_id` column added to 15 high-frequency business tables
  (plot, harvest_record, activity, customer, sales_order, ...). All
  NULLable, no DEFAULT, no index -- zero-impact migration per
  decision #7.
- Initial Albert's Meadows tree seeded (12 nodes):
  GROUP / HQ (Karen Office, Nairobi) + Finance / Legal / Sales /
  FARM Albert's Farm (Isinya, Kajiado) + Farm General Management /
  HR & Admin / Vegetable & Fruit Cultivation / Mushroom Cultivation /
  Input Warehouse / Packhouse with cold storage
- 5 future-proof tags pre-seeded: SEASON.Q2/Q3, GLOBAL_GAP,
  KEBS_FOOD_SAFETY, PCPB_A
- Kang created as Albert's Farm manager: `kang.manager` /
  `Welcome@123456` (must change on first login), bound to MANAGER role

### Added -- ORG Java module

- 5 entities + 5 mappers (OrgNode/Tag/NodeTag/User/DataAccessAudit)
- `OrgNodeService` -- 250 lines with all 8 decisions enforced at
  service layer (physical-cannot-delete, type enum, parent rules,
  cycle prevention, ancestors auto-build, etc.)
- `OrgUserService` -- decision #4 closes prior primary automatically
  when reassigning
- `OrgTagService` -- category whitelist enforced
- 3 REST controllers, 20 endpoints under `/v1/org/*`

### Added -- DataScope subsystem (`@DataScope`)

- `@DataScope(table, column, ...)` annotation on controller methods
- ThreadLocal context stack with auto-cleanup
- `DataScopeService` with Redis-cached subtree id resolution
- `DataScopeInnerInterceptor` (MyBatis-Plus) with 3 strategies:
  - scope=group + <=1000 ids -- `WHERE col IN (...)`
  - scope=group + >1000 ids  -- fallback EXISTS subquery against
    `org_node.ancestors`
  - scope=self -- `WHERE created_by = ?`
  - scope=all -- no rewrite (audited instead)
- `DataAccessAuditAspect` (`@Async`) records every read by a
  data_scope=all user into `data_access_audit` (decision #5)
- Safety: default `agrios.datascope.enabled=false` (decision #7)
  -- annotations exist but the interceptor short-circuits, so all
  v3.4.0 surface continues to behave identically. Production deploy
  flips the flag after grey-out validation.
- Lazy supplier injection breaks the
  `interceptor -> service -> mapper -> sqlSessionFactory` cycle

### Added -- @DataScope wired on 5 high-traffic controllers

- PlotController.list (`table=plot`)
- HarvestRecordController.list (`table=harvest_record`)
- ActivityController.list (`table=activity`)
- CustomerController.list (`table=customer`)
- SalesOrderController.list (`table=sales_order`)

All annotations dormant under `enabled=false` -- no behaviour
change. To activate in production, set `DATASCOPE_ENABLED=true`.

### Added -- frontend org tree management UI

- `/system/org` route under the system menu
- `OrgTreePage.vue` -- 3-column layout (tree / detail / members + tags)
- el-tree with type chips, filter, active toggle
- Create-node dialog with type-aware parent picker
- Member assign dialog (primary + co-manager + effective_from)
- Tag attach / detach inline UI
- Physical node delete button hidden (UI mirrors backend decision #3)
- i18n in en / zh / sw

### Migrations

- `050_org_model.sql` -- 5 new tables + 12-node tree + Kang account +
  15 ALTER + day-1 backfill
- `050_org_model_rollback.sql` -- full reverse script

### Verified

- Migration applies clean on Albert's Farm pilot DB (<10k rows total)
- Backend boot OK with `datascope.enabled=false`
- All 12 nodes / 7 ids in Albert's Farm subtree / Kang primary=10
- Physical-node delete rejected with business error
- CS analytics + CSAT + digest endpoints unchanged from v3.4.0
- `data_access_audit` empty (audit dormant with flag off)

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
