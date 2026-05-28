# 2Africa Product Suite — Port & Container Allocation Matrix

This document is the **single source of truth** for service ports and container
names across all 2Africa products. Keep it updated whenever you add a new
service so the products can coexist on a single dev box and a single VPS
without conflict.

---

## 1. Container naming convention

Pattern: `<product>-<service>` (all lowercase, kebab-case).

| Product            | Prefix          |
|--------------------|-----------------|
| 2Africa AgriOS     | `agrios-`       |
| 2Africa FactoryOS  | `factoryos-`    |
| 2Africa RetailOS   | `retailos-`     |
| 2Africa TravelOS   | `travelos-`     |
| 2Africa LocalOS    | `localos-`      |

Examples: `agrios-mysql`, `factoryos-backend`, `retailos-redis`.

> Legacy: AgriOS currently uses `toafrica-*` in `backend/docker-compose.yml`
> (dev profile). New compose files use `agrios-*`. Rename pending.

---

## 2. Port allocation policy

Each product gets a **100-port band**. Within the band, the offsets are
identical across products so muscle memory works.

| Service offset | Purpose                          |
|----------------|----------------------------------|
| `+0`           | Frontend (Nginx, port 80 in prod) |
| `+80`          | Backend HTTP (Spring Boot)       |
| `+06`          | MySQL                            |
| `+79`          | Redis                            |
| `+00` / `+01`  | MinIO API / Console              |
| `+88`          | Reserved for product-specific extras (worker, scheduler, ...) |

### Product → band assignment

| Product        | Band         | Frontend dev | Backend dev | MySQL | Redis | MinIO API | MinIO Console |
|----------------|--------------|--------------|-------------|-------|-------|-----------|---------------|
| **AgriOS**     | `30xx`       | 3000         | 3080        | 3006  | 3379  | 3090      | 3091          |
| **FactoryOS**  | `31xx`       | 3100         | 3180        | 3106  | 3179  | 3190      | 3191          |
| **RetailOS**   | `32xx`       | 3200         | 3280        | 3206  | 3279  | 3290      | 3291          |
| **TravelOS**   | `33xx`       | 3300         | 3380        | 3306  | 3379  | 3390      | 3391          |
| **LocalOS**    | `34xx`       | 3400         | 3480        | 3406  | 3479  | 3490      | 3491          |

Note: host-side ports follow the band; in-container ports stay at standard defaults (3306 for MySQL, 6379 for Redis, etc.) so JDBC / Redisson URLs inside the docker network do not change.

### Cross-product shared infrastructure (optional, single host)

If you run multiple products on the same VPS, port collisions on the host
side are avoided because every product publishes via its own band. The
in-container service ports (3306 for MySQL, 6379 for Redis, etc.) stay at
defaults inside each product's docker network — only the host-side mapping
changes.

---

## 3. AgriOS — current actual usage

### Development (`backend/docker-compose.yml`)

| Container         | Image              | Host port → Container port | Purpose         |
|-------------------|--------------------|----------------------------|-----------------|
| `toafrica-mysql`  | mysql:8.0          | 3006 → 3306                | DB              |
| `toafrica-redis`  | redis:7-alpine     | 6379 → 6379                | Cache + lock    |
| `toafrica-backend`| agrios-backend:dev | 8080 → 8080                | Spring Boot API |
| `toafrica-minio`  | minio/minio:latest | 9000 → 9000, 9001 → 9001   | Object storage  |
| (frontend)        | (vite dev server)  | 5173 (host process)        | Vite HMR        |

### Production (`docker-compose.prod.yml`)

| Container          | Image                                        | Host port → Container port | Purpose         |
|--------------------|----------------------------------------------|----------------------------|-----------------|
| `agrios-mysql`     | mysql:8.0                                    | (internal only)            | DB              |
| `agrios-redis`     | redis:7-alpine                               | (internal only)            | Cache + lock    |
| `agrios-minio`     | minio/minio:latest                           | (internal only)            | Object storage  |
| `agrios-backend`   | ghcr.io/.../2africa-agrios-backend:latest    | (internal only, 8080)      | Spring Boot API |
| `agrios-frontend`  | ghcr.io/.../2africa-agrios-frontend:latest   | `${FRONTEND_PORT:-80}:80`  | Nginx SPA + /api proxy |

### Monitoring (`docker-compose.monitoring.yml`)

| Container             | Image                  | Host port → Container port | Purpose      |
|-----------------------|------------------------|----------------------------|--------------|
| `agrios-uptime-kuma`  | louislam/uptime-kuma:1 | 127.0.0.1:3001 → 3001      | Status page + alerts |

---

## 4. Recommended target state for AgriOS

To match the band allocation above, rename + retag like this in a future
sprint:

| Old                    | New                |
|------------------------|--------------------|
| `toafrica-mysql`       | `agrios-mysql`     |
| `toafrica-redis`       | `agrios-redis`     |
| `toafrica-backend`     | `agrios-backend`   |
| `toafrica-minio`       | `agrios-minio`     |
| Host port 3306 (mysql) | 3006 (aligned to 30xx band) |
| Host port 6379 (redis) | 3379 |
| Host port 8080 (backend) | 3080 |
| Host port 9000 (minio API) | 3090 |
| Host port 9001 (minio console) | 3091 |
| Host port 3001 (kuma)  | 3001 (shared monitoring, not per-product) |

> Shared services (monitoring, log aggregation, identity provider) live
> outside any product's band. Reserve `3000-3009` for shared monitoring/ops.

---

## 5. New product checklist

Before you start a new product (FactoryOS, RetailOS, TravelOS, LocalOS):

1. Claim a band from §2 (assign a row in the table).
2. Set `container_name:` for every service to `<prefix>-<service>`.
3. Add the host-side port mapping using your band offsets.
4. Update this file with the actual values once they're committed.
5. If the product needs an extra service (e.g. a Python worker), use offset `+88` first; if you need more, add a new row to §2 with the offset documented.

---

## 6. Docker network isolation

Each product's compose file should declare its own default network. They are
automatically isolated by docker, but explicit naming helps debugging:

```yaml
networks:
  default:
    name: agrios_net   # factoryos_net / retailos_net / etc.
```

Cross-product calls (if ever needed) go through the host or a shared
`reverse-proxy_net` overlay — not direct container links.

---

## 7. Reserved 2Africa ranges

For future products beyond the initial five, use these reserved bands so
the table stays predictable:

| Band      | Product slot           |
|-----------|------------------------|
| 30xx      | AgriOS (assigned)      |
| 31xx      | FactoryOS (assigned)   |
| 32xx      | RetailOS (assigned)    |
| 33xx      | TravelOS (assigned)    |
| 34xx      | LocalOS (assigned)     |
| 35xx-37xx | Future products        |
| 38xx      | Shared services (auth/SSO, billing, ...) |
| 39xx      | Shared monitoring/ops  |
