#!/usr/bin/env bash
# ============================================================================
# 2Africa AgriOS - MinIO bucket backup (Sprint 32).
#
# Mirrors the production MinIO bucket to a local backup directory. Optionally
# pushes to an off-site S3-compatible remote via rclone.
#
# Usage:
#   bin/backup-minio.sh
#   BACKUP_DIR=/srv/agrios-backups bin/backup-minio.sh
#
# Designed to run from cron daily, after backup-db.sh:
#   30 3 * * *  cd /opt/agrios && bin/backup-minio.sh >> /var/log/agrios-minio-backup.log 2>&1
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "ERROR: $ENV_FILE not found"; exit 1; }
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

: "${MINIO_ROOT_USER:?MINIO_ROOT_USER not set}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD not set}"
: "${OSS_BUCKET:?OSS_BUCKET not set}"

BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/minio}"
MINIO_CONTAINER="${MINIO_CONTAINER:-agrios-minio}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"

mkdir -p "$BACKUP_DIR"
LOG="$BACKUP_DIR/backup.log"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

log "MinIO backup start"

# ----- Configure mc alias inside the container (idempotent) -----
docker exec "$MINIO_CONTAINER" mc alias set local \
    "http://localhost:9000" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >>"$LOG" 2>&1

# ----- Mirror bucket → tarball -----
ts="$(date +%Y%m%d-%H%M%S)"
staging="$BACKUP_DIR/.staging-$ts"
target="$BACKUP_DIR/minio-${OSS_BUCKET}-${ts}.tar.gz"

# Use mc cp via host bind-mount, falling back to tar through stdout if no mount.
# Simpler approach: docker exec mc mirror into /tmp inside container, then docker cp out.
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/backup-$ts" >/dev/null 2>&1 || true
docker exec "$MINIO_CONTAINER" mkdir -p "/tmp/backup-$ts"

log "Mirroring local/$OSS_BUCKET to container /tmp/backup-$ts"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite "local/$OSS_BUCKET" "/tmp/backup-$ts/" >>"$LOG" 2>&1

# Pack
mkdir -p "$staging"
docker cp "$MINIO_CONTAINER:/tmp/backup-$ts/." "$staging/" >>"$LOG" 2>&1
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/backup-$ts" >/dev/null 2>&1 || true

tar -czf "$target" -C "$staging" . >>"$LOG" 2>&1
rm -rf "$staging"

size=$(stat -c%s "$target" 2>/dev/null || stat -f%z "$target")
log "MinIO archive OK ($(numfmt --to=iec "$size" 2>/dev/null || echo "$size B"))"

# ----- Retention: keep last 7 daily archives -----
cd "$BACKUP_DIR"
# shellcheck disable=SC2012
ls -1t minio-*.tar.gz 2>/dev/null | tail -n +8 | while read -r old; do
    rm -f "$old"
    log "Pruned $old"
done
cd - >/dev/null

# ----- Optional off-site copy via rclone -----
if [[ -n "$RCLONE_REMOTE" ]]; then
    if command -v rclone >/dev/null 2>&1; then
        log "Uploading to $RCLONE_REMOTE ..."
        if rclone copy "$target" "$RCLONE_REMOTE/minio/" >>"$LOG" 2>&1; then
            log "Remote copy OK"
        else
            log "WARN: rclone copy failed"
        fi
    else
        log "WARN: rclone not installed; skipping remote copy"
    fi
fi

log "MinIO backup complete"
