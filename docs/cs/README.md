# CS-Core (Customer Service Core)

> Cross-product customer service module for the 2Africa ecosystem.
>
> First shipped: v3.3.0 (Sprint 48a-d). Status: extracted to a horizontal
> module from inside the AgriOS codebase; not yet a standalone repo.

## 1. What this is

`CS-Core` is the horizontal **customer service layer** shared across all
2Africa products (AgriOS / RetailOS / FactoryOS / TravelOS / AgriCloud /
MarketOS / LocalOS). It owns:

* **Conversations / messages / inboxes / agents** — all the omnichannel
  CSR primitives, brokered through an embedded **Chatwoot v4.13** stack.
* **Channel adapters** — Email (IMAP/SMTP), WhatsApp (Meta Cloud), Web
  Widget, SMS (Africa's Talking + Twilio-compatible API channels).
* **Zero-cost policy engine** — Sprint 47's WhatsApp 24h service-window
  guard, extensible to any per-channel cost rule.
* **Business-context aggregation SPI** — pluggable interface
  (`BusinessContextProvider`) so each consuming product attaches its own
  business entities (Customer / Buyer / Traveler / etc.) to the
  agent-facing sidebar.

## 2. Architecture

```
┌────────────────────────────────────────────────────────────┐
│  Consuming Product Layer (per product)                      │
│                                                             │
│  AgriOS    → AgriOsBusinessContextProvider                  │
│              subjectType="customer"                         │
│              aggregates Sales Order / AR / Complaint        │
│                                                             │
│  RetailOS  → RetailOsBusinessContextProvider                │
│              subjectType="buyer"                            │
│              aggregates OrderItem / Refund / Loyalty        │
│                                                             │
│  ... etc.                                                   │
└─────────────────────┬──────────────────────────────────────┘
                      │  implements BusinessContextProvider
                      ▼
┌────────────────────────────────────────────────────────────┐
│  CS-Core (this module)                                      │
│                                                             │
│  • DB schema: cs_contact_link, cs_event_log                 │
│  • REST API: /v1/cs/* (+ /v1/service/* legacy alias)        │
│  • SPI: BusinessContextProvider, ChannelAdapter (Sprint 49) │
│  • Policy: WhatsAppPolicyService (extensible)               │
│  • UI: Vue 3 components, 5 sub-menus, 3-pane workspace      │
│  • i18n: zh / en / sw                                       │
└─────────────────────┬──────────────────────────────────────┘
                      │  HTTP + ActionCable
                      ▼
┌────────────────────────────────────────────────────────────┐
│  Channel Backend (replaceable)                              │
│                                                             │
│  Default: Chatwoot v4.13 (MIT, embedded as docker service)  │
│  Future:  Tidio / Crisp / proprietary                       │
└────────────────────────────────────────────────────────────┘
```

## 3. Database schema (2 tables)

CS-Core owns exactly **two** tables. Everything else (conversations,
messages, agents, contacts) lives in Chatwoot's own database. The two
CS-Core tables bridge the consuming product's domain entities to
Chatwoot.

### `cs_contact_link`

Bridges a consuming-product entity (customer / buyer / traveler / ...)
to a Chatwoot contact. One row per (chatwoot_account_id,
chatwoot_contact_id) pair.

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT PK | autoincrement |
| `subject_type` | VARCHAR(32) | `customer` / `buyer` / `traveler` / `partner` / `supplier` / `worker` — the kind of entity the contact represents |
| `subject_id` | BIGINT | id of the row in the consuming product's entity table |
| `chatwoot_contact_id` | BIGINT | the matching Chatwoot contact |
| `chatwoot_account_id` | BIGINT | which Chatwoot account (multi-tenant ready) |
| `last_synced_at` | DATETIME | timestamp of last successful sync |
| `sync_status` | VARCHAR(16) | `ok` / `pending` / `error` |
| `sync_error` | VARCHAR(500) | last failure message |

### `cs_event_log`

Cross-system event audit trail. Inbound webhooks from Chatwoot, outbound
actions the consuming product triggered, sync jobs, AI decisions.

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT PK | |
| `event_type` | VARCHAR(64) | `webhook.message_created` / `action.create_complaint` / `sync.customer_push` / `bot.escalated_to_human` |
| `direction` | VARCHAR(16) | `inbound` / `outbound` |
| `subject_type` | VARCHAR(32) | optional, same vocabulary as `cs_contact_link.subject_type` |
| `subject_id` | BIGINT | optional |
| `chatwoot_account_id` | BIGINT | optional |
| `chatwoot_conversation_id` | BIGINT | optional |
| `chatwoot_message_id` | BIGINT | optional |
| `payload` | JSON | the full event body |
| `result` | VARCHAR(16) | `ok` / `failed` / `skipped` |
| `error_message` | VARCHAR(500) | |
| `idempotency_key` | VARCHAR(128) UNIQUE | dedupe duplicate webhooks |

## 4. SPI (Service Provider Interface)

### `BusinessContextProvider`

Each consuming product implements this Spring `@Component` to convert a
`subject_id` into the agent sidebar's "vital signs" block. See
`backend/src/main/java/.../service/spi/BusinessContextProvider.java`.

```java
public interface BusinessContextProvider {
    /** "customer" / "buyer" / "traveler" / etc. */
    String subjectType();

    /** Aggregates open orders / AR / complaints / etc. */
    ConversationDetailVO.BusinessContext forSubject(Long subjectId);
}
```

Multiple providers may coexist in one app context; the controller
dispatches by `subjectType()`.

AgriOS's reference implementation is
`AgriOsBusinessContextProvider`. It aggregates open sales orders,
overdue AR (using `customer.creditDays`), open complaints, and the
customer's last order date.

## 5. REST API surface

CS-Core exposes its endpoints under **two equivalent path prefixes**:

* `/v1/cs/...` — the canonical CS-Core path (use this for new code)
* `/v1/service/...` — legacy alias kept for backward compat (will be
  retired in a future major version)

| Endpoint | Purpose |
|---|---|
| `GET    /v1/cs/conversations` | list with filters (status / assigneeType / inboxId / page) |
| `GET    /v1/cs/conversations/{id}` | detail + messages + business context + WhatsApp policy |
| `POST   /v1/cs/conversations/{id}/messages` | reply or private note (Sprint 47 policy enforcement) |
| `POST   /v1/cs/conversations/{id}/status` | open / resolved / pending / snoozed |
| `POST   /v1/cs/conversations/{id}/assignee` | assign / unassign |
| `GET    /v1/cs/inboxes` | list inboxes |
| `GET    /v1/cs/agents` | list agents |
| `POST   /v1/cs/inboxes/setup-email` | Email channel setup wizard |
| `POST   /v1/cs/inboxes/setup-whatsapp` | WhatsApp Cloud setup wizard |
| `POST   /v1/cs/inboxes/setup-web-widget` | Web widget setup |
| `POST   /v1/cs/inboxes/setup-sms` | SMS / Africa's Talking setup |
| `GET    /v1/cs/sms-templates` | Sprint 45 SMS templates |
| `POST   /v1/cs/sms-templates/render` | render with conversation context |
| `POST   /v1/cs/ai-agent/diagnose` | Sprint 40f AI agent draft |
| `GET    /v1/cs/health` | module health |

Full OpenAPI 3.1 spec: `docs/cs/openapi.yaml`.

## 6. UI: 5 sub-menus + 3-pane workspace

The agent-facing UI sits under one parent menu **客服 / CS / Huduma**:

1. **对话 / Conversations** — 3-pane workspace
   * **Left rail**: Views (All / Mine / Unassigned) + dynamic inbox list
   * **Middle**: status tabs (Open / Pending / Resolved) + conversation
     list
   * **Right**: conversation detail (messages, business context,
     reply composer, AI draft, SMS templates, WhatsApp policy chip)
2. **投诉 / Complaints** — links to product-specific complaint module
   (e.g. AgriOS `/qc/complaints`)
3. **分析 / Analytics** — Sprint 49 stub for ticket volume / SLA / KPIs
4. **团队 / Team** — Sprint 49 stub for agent roster + workload
5. **设置 / Settings** — 4 tabs:
   * **Inboxes** (real, today): per-channel inbox CRUD + setup wizards
   * **AI Agent** (Sprint 49): toggle, model, prompt, auto-reply threshold
   * **Business Hours** (Sprint 49): windows, holidays, after-hours
   * **Reply Policy** (Sprint 49): WhatsApp 24h, SLA, priority

## 7. Future direction (standalone ServiceOS)

CS-Core's long-term path is to become a **standalone product**: the
"2Africa ServiceOS" referenced by the V1.0 PRD. The migration is
incremental:

1. **Today (v3.3.0)** — CS-Core lives inside AgriOS backend / frontend
   as an extractable module.
2. **Sprint 49+** — adopters (RetailOS, FactoryOS) integrate by copying
   schema + implementing the SPI, sharing one Chatwoot deployment.
3. **Future v0.x** — extract into a separate repo `2Africa-ServiceOS`
   with its own port range (`4400-4499`, see `docs/cs/PORTS.md`), its
   own version stream, and its own SDK consumed by all products.

## 8. Related documents

* `INTEGRATION.md` — step-by-step for adopting CS-Core in a new product
* `openapi.yaml` — formal API contract
* `PORTS.md` — port allocation for standalone deployment
* `../../TRADEMARK.md` — codename vs brand boundary (agrios.org safety)
* `../../migrations/047_cs_module_standardization.sql` — schema rename
  migration
