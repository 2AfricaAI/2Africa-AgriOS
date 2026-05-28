# 2Africa AgriOS — Deployment Runbook

This document walks through deploying AgriOS to production. Two paths:

- **A. Single VPS** with docker-compose (Sprint 29, this guide). Good for smoke tests and small production with under ~100 users.
- **B. Kubernetes cluster** with manifests in `k8s/` (Sprint 30, next).

---

## Prerequisites

- Linux server (Ubuntu 22.04 LTS recommended), 2 vCPU / 4 GB RAM minimum
- Docker 24+ and the docker compose v2 plugin
- A registered domain pointing to the server (A record)
- Outbound network access for Maven Central + Docker Hub
- (Recommended) UFW firewall: open 22, 80, 443 only

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER   # log out / back in to take effect
```

---

## A. Single-VPS deployment (docker-compose)

### 1. Clone the repo

```bash
git clone https://github.com/2AfricaAI/2Africa-AgriOS.git
cd 2Africa-AgriOS
```

### 2. Configure secrets

```bash
cp .env.example .env.prod
# Edit .env.prod and replace every CHANGE_ME_* value
nano .env.prod
```

Generate strong secrets:

```bash
# JWT — needs 64+ chars for HS512
openssl rand -base64 64 | tr -d '\n'

# DB / Redis / MinIO passwords — 32 chars hex
openssl rand -hex 32
```

**`OSS_PUBLIC_ENDPOINT`** should be the public HTTPS URL the browser uses to download files. If MinIO is behind your domain, set it to `https://files.farm.example.com`. If you proxy MinIO under the same domain, e.g. `https://farm.example.com/files`.

**`CORS_ALLOWED_ORIGINS`** should list every domain the frontend is served from, comma-separated, no spaces: `https://farm.example.com,https://app.example.com`.

### 3. Build and launch

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f
```

First boot takes 3-5 min (mysql init + maven dependency download + vite build). Watch for:

```
agrios-backend   | Started AgriosApplication in X seconds
agrios-frontend  | nginx: ready
```

### 4. Create the MinIO bucket

```bash
docker exec -it agrios-minio mc alias set local http://localhost:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD}
docker exec -it agrios-minio mc mb local/${OSS_BUCKET}
docker exec -it agrios-minio mc anonymous set download local/${OSS_BUCKET}
```

### 5. Set up Nginx + Let's Encrypt on the host

The `frontend` container listens on port 80 of the host. Put a host-level reverse proxy in front of it to terminate TLS.

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

Create `/etc/nginx/sites-available/agrios`:

```nginx
server {
    listen 80;
    server_name farm.example.com;
    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> If the AgriOS frontend container is on port 80, change `docker-compose.prod.yml` to map `127.0.0.1:8081:80` then point Nginx to `127.0.0.1:8081`.

Enable + obtain cert:

```bash
sudo ln -s /etc/nginx/sites-available/agrios /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d farm.example.com
```

Certbot adds a 443 server block and a cron job for auto-renewal.

### 6. First-login smoke test

Open `https://farm.example.com/login` — default credentials are seeded by `schema.sql`:

- Username: `admin`
- Password: `Admin@123456`

**Change the admin password immediately** via the user profile page.

---

## A.bis CI/CD via GitHub Actions (Sprint 31)

Once the repo is on GitHub, two workflows run automatically:

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/backend.yml`  | PR / push under `backend/**`  | PR: `mvn compile + package`. Push to main / tag: build image, push to `ghcr.io/<owner>/2africa-agrios-backend:<tag>` |
| `.github/workflows/frontend.yml` | PR / push under `frontend/**` | PR: `npm ci + npm run build`. Push to main / tag: build nginx image, push to `ghcr.io/<owner>/2africa-agrios-frontend:<tag>` |

Image tag scheme: `<git-short-sha>`, `latest` (head of main), and `vX.Y.Z` when you push a semver tag.

### One-time setup on GitHub

1. **Make the package public** (recommended for free pulls from the VPS):
   `Settings → Packages → 2africa-agrios-backend → Change visibility → Public`. Repeat for frontend.
2. **(Optional) Set the public base URL** that gets baked into the frontend bundle:
   `Settings → Secrets and variables → Actions → Variables → New variable`:
   - Name: `FRONTEND_PUBLIC_BASE_URL`
   - Value: `https://farm.example.com`

### Deploy on the VPS

Use the bundled helper script — no manual `docker build` on the server.

```bash
cd 2Africa-AgriOS
git pull                                    # only to pick up new .env.example keys
bin/deploy.sh                               # pull :latest for backend + frontend
bin/deploy.sh --tag v1.2.0                  # pin both to a semver tag
bin/deploy.sh --backend abc1234 --frontend def5678   # pin to specific shas
bin/deploy.sh --health                      # health check only
bin/deploy.sh --rollback                    # revert .env.prod to .env.prod.bak and restart
```

The script:
- Backs up `.env.prod` to `.env.prod.bak` (single-step rollback target)
- Rewrites `BACKEND_IMAGE` / `FRONTEND_IMAGE` env vars
- Pulls the new images
- Recreates only `backend` + `frontend` containers (mysql / redis / minio untouched)
- Waits up to 60s for backend health, auto-rollbacks-by-instruction if unhealthy
- Prunes dangling images

### Private images (alternative)

If you keep the packages private, the VPS needs a GitHub Personal Access Token with `read:packages`:

```bash
echo "$GHCR_PAT" | docker login ghcr.io -u <github-username> --password-stdin
```

Then `bin/deploy.sh` works as above.

---

## B. Kubernetes deployment

K8s manifests are deferred — single VPS deployment will scale you to ~100 users. Revisit when:

- Multi-region failover is needed
- HPA-based autoscaling becomes worth the complexity
- Multi-tenant isolation is required

When that day comes, the env-var contract from `.env.example` translates 1:1 to Kubernetes Secret + ConfigMap. High-level checklist for future reference:

1. A working cluster (k3s for single-node, EKS / GKE / AKS for managed)
2. `ingress-nginx` controller installed
3. `cert-manager` + a `ClusterIssuer` pointing at Let's Encrypt
4. DNS A record pointing at the LoadBalancer IP
5. `kubectl create namespace agrios`
6. `kubectl apply -k k8s/overlays/prod`
7. `kubectl -n agrios get pods -w`

---

## Operations

### Backup & restore (Sprint 32)

Daily backups are driven by `cron` calling the scripts in `bin/`. Two backups run nightly:

- **03:00** — `bin/backup-db.sh` dumps MySQL → `backups/db/daily/agrios-YYYYMMDD-HHMMSS.sql.gz`
- **03:30** — `bin/backup-minio.sh` mirrors the MinIO bucket → `backups/minio/minio-<bucket>-YYYYMMDD.tar.gz`

Retention policy (DB):

| Tier    | Kept |
|---------|------|
| Daily   | last **7** |
| Weekly  | last **4** (each Sunday's backup promoted) |
| Monthly | last **12** (each 1st-of-month backup promoted) |

Retention policy (MinIO): last **7** daily tarballs.

#### One-time install on the server

```bash
sudo cp ops/cron.agrios-backup /etc/cron.d/agrios-backup
sudo chown root:root /etc/cron.d/agrios-backup
sudo chmod 644 /etc/cron.d/agrios-backup
sudo systemctl restart cron
```

Edit the file to fix `/opt/agrios` to your actual checkout path. Verify:

```bash
grep CRON /var/log/syslog | tail -20
# Or, manually run it once:
sudo -u root bash -c 'cd /opt/agrios && bin/backup-db.sh'
ls -la /opt/agrios/backups/db/daily/
```

#### Off-site copy (optional but recommended)

Set `RCLONE_REMOTE` in `.env.prod` to push every backup to S3 / R2 / B2:

```bash
sudo apt install -y rclone
rclone config             # configure remote, e.g. name it 'r2'
# Then in .env.prod:
RCLONE_REMOTE=r2:agrios-backups
```

Both `backup-db.sh` and `backup-minio.sh` will pick it up automatically.

#### Manual ad-hoc backup

```bash
cd /opt/agrios
bin/backup-db.sh
bin/backup-minio.sh
```

#### Restore

The restore script is interactive and **always takes a safety snapshot of the current DB before overwriting**:

```bash
cd /opt/agrios

# Interactive: pick from the 10 most recent backups
bin/restore-db.sh

# Restore from a specific file
bin/restore-db.sh backups/db/daily/agrios-20260601-030000.sql.gz

# Restore the most recent daily backup
bin/restore-db.sh --latest

# Dry-run (prints the command it would execute)
bin/restore-db.sh --dry-run --latest
```

After the restore, the script prints a verification (table count + admin user) so you can spot a bad restore immediately.

#### Restore drill (do this once a quarter!)

A backup you've never restored is a wish, not a backup. Quarterly drill:

1. `bin/backup-db.sh` — fresh backup
2. `bin/restore-db.sh --latest` — overwrite live DB with itself
3. Spot-check a few records: log in, open a complaint, open a recall PDF
4. If anything looks wrong, the pre-restore safety snapshot is at `backups/db/pre-restore/`

#### MinIO restore (manual)

```bash
# Extract a tarball into the bucket
tar -xzf backups/minio/minio-2africa-agrios-20260601-033000.tar.gz -C /tmp/minio-restore
docker exec -i agrios-minio mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD
docker cp /tmp/minio-restore/. agrios-minio:/tmp/restore/
docker exec agrios-minio mc mirror --overwrite /tmp/restore/ local/2africa-agrios/
```

### Rolling update

```bash
cd 2Africa-AgriOS && git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend frontend
```

### Tail logs

```bash
docker compose -f docker-compose.prod.yml logs -f --tail 200 backend
```

### Drop & recreate (DESTRUCTIVE)

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod down -v
```

`-v` deletes named volumes including the database. Make sure you have a backup first.

---

### Monitoring & alerting (Sprint 33)

A lightweight self-hosted monitor — **Uptime Kuma** — runs as its own docker stack so you can bounce it independently of the app.

#### Start it

```bash
cd /opt/agrios
docker compose -f docker-compose.monitoring.yml up -d
docker compose -f docker-compose.monitoring.yml logs -f
```

It binds to `127.0.0.1:3001` by default — front it with the host Nginx if you want a public status page (`status.farm.example.com`):

```nginx
server {
    listen 80;
    server_name status.farm.example.com;
    location / {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Then `sudo certbot --nginx -d status.farm.example.com`.

#### First-visit setup

Open the UI → create the admin user **immediately** (before exposing publicly) → log in.

#### Recommended monitors

Add these from the Uptime Kuma UI. All can fire alerts to email / Slack / Telegram / Discord / generic webhook.

| Name | Type | Target | Heartbeat | Notes |
|------|------|--------|-----------|-------|
| **Site (HTTPS)**     | HTTP(s)        | `https://farm.example.com/` | 60s | Expect 200; uses cert check |
| **Site SSL expiry**  | (auto)         | (built into HTTP monitor)   | -    | Alerts 14 days before expiry |
| **Backend health**   | HTTP(s) - JSON | `https://farm.example.com/api/actuator/health` | 60s | JSON contains `"status":"UP"` |
| **Backend live**     | HTTP(s)        | `https://farm.example.com/api/actuator/health/liveness` | 30s | Expect 200, faster recovery signal |
| **DB port**          | TCP Port       | `127.0.0.1:3306` from the *host*, or use Docker network | 60s | Catches db crash before app does |
| **MinIO health**     | HTTP(s)        | `https://files.farm.example.com/minio/health/live` | 120s | If MinIO is publicly served |
| **Disk space**       | Push           | (push from cron — see below) | 5min | Custom; alerts when free space &lt; 10% |

#### Alert channels

In Uptime Kuma → **Settings → Notifications**. Recommended at minimum:

1. **Email** (SMTP — Gmail App Password works, but provider-aware like SendGrid/Mailgun is more reliable)
2. **Slack / Telegram / Discord webhook** — phone-pinging

Wire each monitor to both, so if email is slow you still get the push.

#### Disk-space push monitor

Add a "Push" type monitor in Uptime Kuma, copy its URL, then add to crontab:

```bash
# Every 5 minutes, push only if free space > 10%
*/5 * * * *  root  test $(df / | awk 'NR==2 {print int($5)}') -lt 90 && curl -fsS "https://status.farm.example.com/api/push/<token>?status=up&msg=ok&ping="
```

If the disk fills up, the curl doesn't fire, Uptime Kuma marks the monitor down after the configured heartbeat, and you get alerted.

#### Status page

Create a public Status Page in Uptime Kuma listing the public-facing monitors (site, backend health, SSL). This gives customers / colleagues something to check during an outage. URL is `https://status.farm.example.com/status/agrios`.

---

## Security hardening checklist

- [ ] All `CHANGE_ME_*` values in `.env.prod` replaced with strong randoms
- [ ] Admin password rotated after first login
- [ ] Firewall allows only 22 / 80 / 443
- [ ] SSH key auth only, password login disabled
- [ ] MySQL port NOT exposed to host (`ports:` section commented out)
- [ ] MinIO console (port 9001) NOT exposed publicly
- [ ] Daily backups scheduled and tested with a restore drill
- [ ] Logs forwarded to long-term storage (Loki / CloudWatch / Datadog)
- [ ] Monitoring + alerting set up (Sprint 33)
- [ ] Read-only DB user for ad-hoc queries (not `root`)
- [ ] HTTPS enforced; HSTS header set by the ingress
