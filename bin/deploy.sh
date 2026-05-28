#!/usr/bin/env bash
# ============================================================================
# 2Africa AgriOS - VPS deploy helper (Sprint 31).
#
# Pulls the requested image tags from GHCR and restarts the stack with
# docker compose. Use with .env.prod that you keep on the server.
#
# Usage:
#   bin/deploy.sh                       # pull `latest` for both, recreate
#   bin/deploy.sh --tag v1.2.0          # pin both backend + frontend to v1.2.0
#   bin/deploy.sh --backend abc1234 --frontend def5678   # per-service SHA
#   bin/deploy.sh --rollback            # rolls back to the previous .env.prod
#   bin/deploy.sh --health              # health-check only, no restart
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE=".env.prod"
COMPOSE_FILE="docker-compose.prod.yml"
BACKUP_FILE=".env.prod.bak"

[ -f "$ENV_FILE" ] || { echo "ERROR: $ENV_FILE not found. Copy .env.example and fill secrets first."; exit 1; }
[ -f "$COMPOSE_FILE" ] || { echo "ERROR: $COMPOSE_FILE not found."; exit 1; }

BACKEND_TAG=""
FRONTEND_TAG=""
SAME_TAG=""
ROLLBACK=0
HEALTH_ONLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)        SAME_TAG="$2"; shift 2 ;;
    --backend)    BACKEND_TAG="$2"; shift 2 ;;
    --frontend)   FRONTEND_TAG="$2"; shift 2 ;;
    --rollback)   ROLLBACK=1; shift ;;
    --health)     HEALTH_ONLY=1; shift ;;
    -h|--help)    sed -n '4,15p' "$0"; exit 0 ;;
    *)            echo "Unknown arg: $1"; exit 1 ;;
  esac
done

# ----- Rollback -----
if [[ $ROLLBACK -eq 1 ]]; then
  [ -f "$BACKUP_FILE" ] || { echo "ERROR: no $BACKUP_FILE to roll back to."; exit 1; }
  cp "$BACKUP_FILE" "$ENV_FILE"
  echo ">> Rolled $ENV_FILE back to previous version. Restarting..."
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d backend frontend
  exit 0
fi

# ----- Health-only -----
if [[ $HEALTH_ONLY -eq 1 ]]; then
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
  echo ""
  echo ">> Backend /actuator/health:"
  docker exec agrios-backend wget -qO- http://localhost:8080/api/actuator/health || echo "unreachable"
  echo ""
  echo ">> Frontend /healthz:"
  docker exec agrios-frontend wget -qO- http://localhost/healthz || echo "unreachable"
  exit 0
fi

# ----- Decide tags -----
[ -n "$SAME_TAG" ] && { BACKEND_TAG="$SAME_TAG"; FRONTEND_TAG="$SAME_TAG"; }

# Backup current env file before mutating it
cp "$ENV_FILE" "$BACKUP_FILE"

if [[ -n "$BACKEND_TAG" ]]; then
  sed -i.tmp -E "s|^BACKEND_IMAGE=.*$|BACKEND_IMAGE=ghcr.io/2africaai/2africa-agrios-backend:${BACKEND_TAG}|" "$ENV_FILE"
fi
if [[ -n "$FRONTEND_TAG" ]]; then
  sed -i.tmp -E "s|^FRONTEND_IMAGE=.*$|FRONTEND_IMAGE=ghcr.io/2africaai/2africa-agrios-frontend:${FRONTEND_TAG}|" "$ENV_FILE"
fi
rm -f "$ENV_FILE.tmp"

# Print effective config
echo ">> Effective image config:"
grep -E '^(BACKEND_IMAGE|FRONTEND_IMAGE)=' "$ENV_FILE"

# ----- Pull + restart -----
echo ""
echo ">> Pulling latest images..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull backend frontend

echo ""
echo ">> Recreating containers..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d backend frontend

# ----- Wait for backend health -----
echo ""
echo ">> Waiting for backend to become healthy..."
for i in $(seq 1 60); do
  status=$(docker inspect -f '{{.State.Health.Status}}' agrios-backend 2>/dev/null || echo "starting")
  if [[ "$status" == "healthy" ]]; then
    echo "   backend: healthy (after ${i}s)"
    break
  fi
  if [[ $i -eq 60 ]]; then
    echo "   ERROR: backend did not become healthy in 60s"
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail 40 backend
    echo ""
    echo ">> Rolling back. Run with --rollback to revert."
    exit 1
  fi
  sleep 1
done

# ----- Clean up dangling images -----
docker image prune -f >/dev/null

echo ""
echo ">> Deploy complete. ${BACKUP_FILE} kept for rollback."
