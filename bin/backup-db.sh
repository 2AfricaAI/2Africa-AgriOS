#!/usr/bin/env bash
# ============================================================================
# 2Africa AgriOS - MySQL backup (Sprint 32).
#
# What it does:
#   1. mysqldump the production database from inside the mysql container
#   2. Compress with gzip
#   3. Write to $BACKUP_DIR with a dated filename:
#      agrios-YYYYMMDD-HHMMSS.sql.gz
#   4. Apply retention policy:
#      - Keep last 7 daily backups
#      - Keep last 4 weekly backups (Sundays)
#      - Keep last 12 monthly backups (1st of month)
#   5. Optionally upload to S3-compatible remote via rclone (if RCLONE_REMOTE set)
#   6. Log success/failure to $BACKUP_DIR/backup.log
#
# Designed to be run from cron daily, e.g.:
#   0 3 * * *  cd /opt/agrios && bin/backup-db.sh >> /var/log/agrios-backup.log 2>&1
#
# Required env vars (sourced from .env.prod by default):
#   MYSQL_ROOT_PASSWORD  DB_NAME
# Optional:
#   BACKUP_DIR           default: ./backups/db
#   MYSQL_CONTAINER      default: agrios-mysql
#   RCLONE_REMOTE        e.g. "r2:agrios-backups" — empty disables remote copy
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "ERROR: $ENV_FILE not found"; exit 1; }

# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD not set}"
: "${DB_NAME:?DB_NAME not set}"

BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/db}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-agrios-mysql}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"

mkdir -p "$BACKUP_DIR/daily" "$BACKUP_DIR/weekly" "$BACKUP_DIR/monthly"
LOG="$BACKUP_DIR/backup.log"

now="$(date +%Y%m%d-%H%M%S)"
dow="$(date +%u)"          # 1=Mon, 7=Sun
dom="$(date +%d)"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

# ----- 1. Dump -----
file="agrios-${now}.sql.gz"
target="$BACKUP_DIR/daily/$file"

log "Backup start -> $target"

if ! docker exec "$MYSQL_CONTAINER" sh -c \
    "mysqldump -uroot -p\"$MYSQL_ROOT_PASSWORD\" --single-transaction --routines --triggers --events $DB_NAME" \
    2>>"$LOG" | gzip > "$target"; then
    log "FAIL: mysqldump exited non-zero"
    rm -f "$target"
    exit 1
fi

# Sanity check: dump should be > 1KB
size=$(stat -c%s "$target" 2>/dev/null || stat -f%z "$target")
if [[ $size -lt 1024 ]]; then
    log "FAIL: dump too small ($size bytes); removing"
    rm -f "$target"
    exit 1
fi
log "OK daily ($(numfmt --to=iec "$size" 2>/dev/null || echo "$size B"))"

# ----- 2. Promote to weekly (Sundays) and monthly (day 01) -----
if [[ "$dow" == "7" ]]; then
    cp -p "$target" "$BACKUP_DIR/weekly/$file"
    log "Promoted to weekly"
fi
if [[ "$dom" == "01" ]]; then
    cp -p "$target" "$BACKUP_DIR/monthly/$file"
    log "Promoted to monthly"
fi

# ----- 3. Retention -----
# Keep newest N files in each tier, delete the rest.
prune() {
    local dir="$1"  keep="$2"
    # ls -1t lists newest first; tail -n +N+1 drops the first N
    if cd "$dir" 2>/dev/null; then
        # shellcheck disable=SC2012
        ls -1t agrios-*.sql.gz 2>/dev/null | tail -n +$((keep + 1)) | while read -r old; do
            rm -f "$old"
            log "Pruned $dir/$old"
        done
        cd - >/dev/null
    fi
}
prune "$BACKUP_DIR/daily"   7
prune "$BACKUP_DIR/weekly"  4
prune "$BACKUP_DIR/monthly" 12

# ----- 4. Optional off-site copy via rclone -----
if [[ -n "$RCLONE_REMOTE" ]]; then
    if ! command -v rclone >/dev/null 2>&1; then
        log "WARN: rclone not installed; skipping remote copy"
    else
        log "Uploading to $RCLONE_REMOTE ..."
        if rclone copy "$target" "$RCLONE_REMOTE/daily/" >>"$LOG" 2>&1; then
            log "Remote copy OK"
        else
            log "WARN: rclone copy failed"
        fi
    fi
fi

log "Backup complete"
