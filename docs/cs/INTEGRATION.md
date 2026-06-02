# CS-Core Integration Guide

> How to adopt CS-Core in a new 2Africa product (RetailOS / FactoryOS /
> TravelOS / AgriCloud / MarketOS / LocalOS).
>
> Audience: backend lead of the consuming product. Estimated effort:
> **2-3 working days** for a minimal integration, **1 week** for a polished
> one with localized business-context aggregator.

## TL;DR — 7 steps

1. Copy 2 DB tables (`cs_contact_link`, `cs_event_log`) into your schema
2. Allocate a port range from `PORT_REGISTRY.md` and add Chatwoot to
   your `docker-compose.yml`
3. Copy the `service/*` backend module (Java) into your codebase
4. Implement `BusinessContextProvider` for your product's domain entity
5. Copy the `views/service/*` Vue module into your frontend
6. Add the 5-submenu CS group to your sidebar
7. Run E2E: send a test message → it appears in CS workspace → reply

Details below.

---

## 0. Prerequisites

* Spring Boot 3.x (or any JVM web framework — translate as needed)
* MySQL 8 or Postgres (CS-Core's two tables are vendor-neutral)
* Vue 3 + Vite + Element Plus (or translate to your stack)
* A Chatwoot v4.13 deployment you can embed via docker-compose
* JWT or session-based auth that puts a bearer token on every request

## 1. Database schema

Copy the canonical schema for the two CS-Core tables:

```sql
-- See migrations/047_cs_module_standardization.sql for the idempotent
-- rename helper if you're upgrading from a "service_*"-named legacy.

CREATE TABLE `cs_contact_link` (
  `id`                       BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `subject_type`             VARCHAR(32)  NOT NULL DEFAULT 'customer',
  `subject_id`               BIGINT       NOT NULL,
  `chatwoot_contact_id`      BIGINT       NOT NULL,
  `chatwoot_account_id`      BIGINT       NOT NULL,
  `last_synced_at`           DATETIME     NULL,
  `sync_status`              VARCHAR(16)  NOT NULL DEFAULT 'pending',
  `sync_error`               VARCHAR(500) NULL,
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_chatwoot_contact` (`chatwoot_account_id`, `chatwoot_contact_id`),
  KEY `idx_subject` (`subject_type`, `subject_id`),
  KEY `idx_sync_status` (`sync_status`)
);

CREATE TABLE `cs_event_log` ( ... -- see AgriOS schema.sql §9 );
```

Pick your product's `subject_type` value once and use it everywhere:

| Product | Recommended `subject_type` |
|---|---|
| AgriOS | `customer` (already shipped) |
| RetailOS | `buyer` |
| FactoryOS | `client` |
| TravelOS | `traveler` |
| AgriCloud | `customer` (same as AgriOS) |
| MarketOS | `seller` or `buyer` (depends on workflow) |

## 2. Allocate a port range

Per `PORT_REGISTRY.md` §2, each product owns a `XX00-XX99` range. CS-Core
itself does **not** need new host ports inside your product — it reuses
your existing API server. But the embedded Chatwoot needs:

* Postgres (sub-slot `X20`)
* Redis (sub-slot `X30`)
* Chatwoot Rails web (a port you pick from `X40-X49`, e.g. `3441` for
  AgriCloud, `3541` for RetailOS post-migration)
* Chatwoot Sidekiq worker (no host port)

Copy the AgriOS `backend/docker-compose.yml` Chatwoot block and renumber
ports to your range. Update `chatwoot.env` accordingly.

If you eventually deploy CS-Core as a **standalone ServiceOS** service
(see `PORTS.md`), it goes in `4400-4499`.

## 3. Copy the Java backend module

From the AgriOS repo, copy these into your product's source tree:

```
backend/src/main/java/.../service/
├── client/              # ChatwootClient, ChatwootContact DTO, etc.
├── config/              # WhatsAppPolicyProperties, etc.
├── controller/          # ConversationController, InboxSetupController, ChatwootWebhookController, ...
├── entity/              # CsContactLink, CsEventLog
├── mapper/              # CsContactLinkMapper, CsEventLogMapper
├── service/             # AiAgentService, ContactSyncService, InboxSetupService, WhatsAppPolicyService, CsEventLogger
├── spi/                 # BusinessContextProvider interface (DO NOT modify)
└── spi/impl/            # YOUR product's BusinessContextProvider implementation
```

**Things to change in the copied code**:

* Java package: rename `ai.toafrica.agrios.service` to your product's
  equivalent (e.g. `ai.toafrica.retailos.cs`)
* Imports of AgriOS-specific business entities (`Customer`, `SalesOrder`,
  `Complaint`) → replace with your product's entities

**Things to NOT change**:

* The `BusinessContextProvider` interface signature
* The DB column names (`subject_type`, `subject_id`)
* The REST paths (`/v1/cs/*` and `/v1/service/*` legacy alias)

## 4. Implement BusinessContextProvider

Take AgriOS's `AgriOsBusinessContextProvider` as your template. Replace
the 4 metrics with what makes sense for your product's customer:

```java
@Component
@RequiredArgsConstructor
public class RetailOsBusinessContextProvider implements BusinessContextProvider {

    private final BuyerMapper buyerMapper;
    private final OrderItemMapper orderMapper;
    private final RefundMapper refundMapper;

    @Override
    public String subjectType() {
        return "buyer";   // matches the value you write to cs_contact_link
    }

    @Override
    public BusinessContext forSubject(Long subjectId) {
        if (subjectId == null) return BusinessContext.builder().build();
        Buyer b = buyerMapper.selectById(subjectId);
        if (b == null) return BusinessContext.builder().build();

        int openOrders = orderMapper.countByBuyerAndStatus(b.getId(), "open");
        int openRefunds = refundMapper.countByBuyerAndStatus(b.getId(), "pending");
        BigDecimal lifetimeValue = orderMapper.sumLifetimeValue(b.getId());
        LocalDate lastOrderDate = orderMapper.findLastOrderDate(b.getId());

        return BusinessContext.builder()
                .openOrderCount(openOrders)
                .openComplaintCount(openRefunds)       // reuse the field for refunds
                .overdueArAmount(lifetimeValue)        // reuse the field for LTV
                .lastOrderDate(lastOrderDate)
                .build();
    }
}
```

> The `BusinessContext` VO is intentionally generic. If your product has
> wildly different metrics, propose a v2 schema in the CS-Core repo (or
> upcoming ServiceOS repo) so all products benefit.

## 5. Copy the Vue UI

From AgriOS, copy:

```
frontend/src/
├── api/service.js                 # rename to cs.js if you prefer
├── views/service/                 # whole directory
│   ├── ConversationList.vue       # 3-pane workspace
│   ├── ConversationDetail.vue     # detail + reply composer
│   ├── InboxList.vue              # CRUD
│   ├── InboxSetupWizard.vue       # channel wizards
│   ├── Settings.vue               # 4-tab settings hub
│   ├── TeamList.vue / Analytics.vue / ComplaintAnalysis.vue  # stubs
│   ├── setup/                     # 4 channel setup steps
│   └── stubs/StubPage.vue         # reusable placeholder component
└── i18n/locales/                  # copy cs.* keys (zh / en / sw)
```

Adjust the API base path if your backend uses a different prefix than
`/v1/cs/*`.

## 6. Sidebar menu

Add to your `AppLayout.vue` (or equivalent):

```vue
<el-sub-menu index="cs">
  <template #title>
    <el-icon><ServiceIcon /></el-icon>
    <span>{{ t('menu.customerService') }}</span>
  </template>
  <el-menu-item index="/service">{{ t('menu.csConversations') }}</el-menu-item>
  <el-menu-item index="/service/complaints">{{ t('menu.csComplaints') }}</el-menu-item>
  <el-menu-item index="/service/analytics">{{ t('menu.csAnalytics') }}</el-menu-item>
  <el-menu-item index="/service/team">{{ t('menu.csTeam') }}</el-menu-item>
  <el-menu-item index="/service/settings">{{ t('menu.csSettings') }}</el-menu-item>
</el-sub-menu>
```

Register the 4 new routes in your router (see AgriOS `router/index.js`).

## 7. End-to-end verification

1. Boot your stack (`docker compose up -d`)
2. Open the CS workspace (`/service`)
3. From a real channel (or the Chatwoot API channel for a simulated
   WhatsApp), send a test message
4. Confirm it appears in the conversation list under the right inbox
5. Open the conversation — the right pane should show your product's
   business-context block (driven by your `BusinessContextProvider`)
6. Reply (public) and reply (private note) — both should succeed
7. If using WhatsApp: backdate the inbound 25h and confirm public
   reply is blocked with code `40901`

A working E2E script for the WhatsApp policy test lives at
`backend/scripts/test-whatsapp-policy.ps1` (PowerShell, Windows-tested).

## 8. Common pitfalls

* **Forgot to rename the Java package** — leads to bean conflicts when
  multiple products with `ai.toafrica.agrios.*` packages run on one
  classpath. Rename `agrios` to your codename.
* **Different `subject_type` strings in DB vs SPI** — if your `cs_contact_link`
  rows say `buyer` but your provider's `subjectType()` returns
  `customer`, the controller silently falls back to an empty business
  context. Pick one string and grep for it.
* **Wrong Chatwoot port in `chatwoot.env`** — must match the host port
  you exposed in docker-compose, not the container's internal 3000.
* **CORS misconfigured for Webhooks** — Chatwoot calls back to your
  product's `/v1/cs/webhook/*`; make sure your Spring Security / nginx
  doesn't strip it.

## 9. Help

* Architecture doc: `README.md` (this directory)
* API contract: `openapi.yaml`
* Port allocation: `PORTS.md`
* AgriOS reference impl: <https://github.com/2AfricaAI/2Africa-AgriOS>
  (look at the `service/` package + `views/service/` directory)
