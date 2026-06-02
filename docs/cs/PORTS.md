# CS-Core / ServiceOS — Port Allocation

> Reference for **standalone deployment** of CS-Core as the "2Africa
> ServiceOS" product. While CS-Core lives inside a consuming product
> (today, AgriOS) it uses the host product's ports; no new host ports
> are needed.
>
> This document follows the convention in the cross-product
> `PORT_REGISTRY.md` (Founder, 2026-06-01). When ServiceOS is extracted
> into its own repo, this file moves there and `PORT_REGISTRY.md` gets a
> new §3 entry pointing here.

## §1 Allocated range: `4400-4499`

| Range | Owner | Status | Notes |
|---|---|---|---|
| `4400-4499` | **2Africa ServiceOS** | 🟡 **Proposed (reserved)** | After Phase 2 SmartOS 3900-3999 + MarketOS 3700-3799 + LocalOS 3800-3899; sits in the 4000 block which has historically been free on dev laptops |

### Rationale

* `4000-4099` is widely used as "dev second-app" port (Sentry, Grafana)
  → avoid
* `4100-4399` is fragmented across various dev tools (4200 = Angular
  dev, 4300 = Jest debugger) → avoid
* `4400-4499` is contiguous, far from common defaults, no known clash
  with major dev tooling
* `4500-4999` reserved for future siblings (HRPlatform / EduOS / ...)

## §2 Within-range slot convention

Following the registry's sub-slot pattern (§2):

| Slot offset | Category | ServiceOS service |
|---|---|---|
| **4400-4409** | Web / frontend | ServiceOS web (Vue / Vite dev `:4400`, prod build behind nginx) |
| **4410-4419** | API | Spring Boot (or Java backend) `:4410` |
| **4420-4429** | Primary DB | Postgres `:4420` |
| **4425** | pgvector / embeddings | `:4425` for RAG / FAQ matching |
| **4430-4439** | Cache + queue | Redis `:4430` |
| **4431** | BullMQ / Sidekiq queue Redis (if separate) | `:4431` |
| **4440-4449** | AI / LLM gateway | LiteLLM proxy `:4440`; AI usage logger `:4441` |
| **4441** | Chatwoot Rails web | `:4441` (replaces the AgriOS-grandfathered `3000`) |
| **4442** | Chatwoot Sidekiq worker | (no host port) |
| **4443** | Chatwoot Postgres | `:4443` |
| **4444** | Chatwoot Redis | `:4444` |
| **4450-4459** | Mail / SMTP / notifications | Mailpit `:4450`, SES adapter `:4451`, WhatsApp gateway `:4452` |
| **4460-4469** | Object storage | MinIO S3 API `:4460`; MinIO console `:4461` |
| **4470-4479** | Observability | Sentry relay `:4470`; Prometheus `:4471`; Grafana `:4472`; Loki `:4473` |
| **4480-4489** | Testing / dev tools | Storybook `:4480`; Playwright runner `:4481` |
| **4490-4499** | Messaging / workflow / health | Kafka `:4490`; Temporal `:4491`; health aggregator `:4499` |

## §3 Container naming

Per registry §5, use `serviceos-*` prefix:

| Container | Image | Internal port | Host port |
|---|---|---|---|
| `serviceos-web` | (Vue + nginx) | 80 | 4400 |
| `serviceos-api` | Spring Boot | 8080 | 4410 |
| `serviceos-postgres` | postgres:16 | 5432 | 4420 |
| `serviceos-pgvector` | pgvector/pgvector:pg16 | 5432 | 4425 |
| `serviceos-redis` | redis:7-alpine | 6379 | 4430 |
| `serviceos-chatwoot-web` | chatwoot/chatwoot:v4.13.0 | 3000 | 4441 |
| `serviceos-chatwoot-worker` | chatwoot/chatwoot:v4.13.0 | — | — |
| `serviceos-chatwoot-postgres` | pgvector/pgvector:pg16 | 5432 | 4443 |
| `serviceos-chatwoot-redis` | redis:7-alpine | 6379 | 4444 |
| `serviceos-mailpit` | axllent/mailpit | 8025/1025 | 4450 / 1025 |
| `serviceos-minio` | minio/minio | 9000/9001 | 4460 / 4461 |

`1025` SMTP for Mailpit is the one exception — outside the range
because SMTP libraries hardcode 1025, same as the registry §2
exception list.

## §4 PostgreSQL DB naming

Per registry §6:

| Database | Owner |
|---|---|
| `serviceos_main` | ServiceOS application data (`cs_contact_link`, `cs_event_log`, future shared tables) |
| `serviceos_chatwoot` | Embedded Chatwoot (managed by Chatwoot itself) |
| `serviceos_test` | CI / test fixtures |

## §5 Forbidden ports (do not reuse)

Inherited from registry §4 — ServiceOS must avoid:

* `3000` (AgriOS Chatwoot, grandfathered)
* `3006` (AgriOS MySQL)
* `5173` (AgriOS Vite dev, grandfathered)
* `6379` (AgriOS Redis, grandfathered)
* `8080` (AgriOS Spring Boot, grandfathered)
* `9000` / `9001` (AgriOS MinIO, grandfathered)

Also avoid every other sibling-OS range (3300, 3400, 3500, 3600, 3700,
3800, 3900) so all 2Africa products + ServiceOS can run on one laptop.

## §6 Migration plan when CS-Core extracts to standalone ServiceOS

1. Create new repo `2Africa-ServiceOS` (org `2AfricaAI`)
2. Copy the `service/*` backend module + `views/service/*` frontend
   module + `docs/cs/*` from AgriOS
3. Renumber AgriOS's grandfathered ports to none — the AgriOS-internal
   CS-Core code keeps working there during the transition
4. Boot the standalone instance on `4400-4499`, configure Chatwoot
   webhook to call `serviceos-api:4410`
5. Update `PORT_REGISTRY.md` §3 to add ServiceOS as a first-class
   product
6. Mark this `PORTS.md` as **active** (instead of **proposed reserved**)

## §7 Health checks

* `GET http://localhost:4410/api/actuator/health` — ServiceOS backend
* `GET http://localhost:4441/api/v1/csrf_token` — Chatwoot web (any
  2xx/3xx counts as healthy, per the Sprint 46 fix)
* `GET http://localhost:4400/healthz` — ServiceOS web (static check)
* `GET http://localhost:4499/` — health aggregator (returns JSON of
  all subsystem statuses, useful for k8s readiness probes)

## §8 Environment policy

| Env | Host ports exposed | Notes |
|---|---|---|
| **Dev (local)** | All `4400-4499` exposed | Direct debugging |
| **Staging** | Only `:443` via Caddy/Cloudflare | E2E + customer demo |
| **Production** | Only ALB/Ingress `:443` + dedicated S3 endpoint(s) | k8s ingress routes to internal cluster IPs |

Matches the registry §7 convention used by every other 2Africa product.
