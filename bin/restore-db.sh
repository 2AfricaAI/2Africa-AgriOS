#!/usr/bin/env bash
# ============================================================================
# 2Africa AgriOS - MySQL restore (Sprint 32).
#
# Restore the production database from a backup file produced by backup-db.sh.
#
# Usage:
#   bin/restore-db.sh                                # interactive: pick from latest 10
#   bin/restore-db.sh backups/db/daily/agrios-20260601-030000.sql.gz
#   bin/restore-db.sh --latest                       # newest daily backup
#   bin/restore-db.sh --dry-run <file>               # show what would happen
#
# Safety:
#   - Refuses to run without explicit confirmation ("YES" must be typed)
#   - Dumps the CURRENT db to a "pre-restore" backup first
#   - Sources .env.prod for credentials
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

DRY_RUN=0
FILE=""
PICK_LATEST=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run) DRY_RUN=1; shift ;;
        --latest)  PICK_LATEST=1; shift ;;
        -h|--help) sed -n '4,17p' "$0"; exit 0 ;;
        *)         FILE="$1"; shift ;;
    esac
done

# ----- Resolve file -----
if [[ $PICK_LATEST -eq 1 ]]; then
    FILE=$(find "$BACKUP_DIR/daily" -name "agrios-*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null \
           | sort -rn | head -n1 | cut -d' ' -f2-)
    [ -n "$FILE" ] || { echo "ERROR: no daily backups found in $BACKUP_DIR/daily"; exit 1; }
elif [[ -z "$FILE" ]]; then
    echo "Available backups (newest first):"
    mapfile -t candidates < <(find "$BACKUP_DIR" -name "agrios-*.sql.gz" -type f -printf '%T@ %p\n' \
                              | sort -rn | head -10 | cut -d' ' -f2-)
    [ ${#candidates[@]} -gt 0 ] || { echo "  (none found)"; exit 1; }
    for i in "${!candidates[@]}"; do
        size=$(stat -c%s "${candidates[$i]}" 2>/dev/null || stat -f%z "${candidates[$i]}")
        printf "  [%d] %s  (%s)\n" "$((i + 1))" "${candidates[$i]}" "$(numfmt --to=iec "$size" 2>/dev/null || echo "$size")"
    done
    read -rp "Pick a number: " idx
    FILE="${candidates[$((idx - 1))]}"
fi

[ -f "$FILE" ] || { echo "ERROR: file not found: $FILE"; exit 1; }

echo ""
echo "Restore plan:"
echo "  Source:      $FILE"
echo "  Target DB:   $DB_NAME (in container $MYSQL_CONTAINER)"
echo "  Dry-run:     $([[ $DRY_RUN -eq 1 ]] && echo YES || echo no)"

if [[ $DRY_RUN -eq 1 ]]; then
    echo ""
    echo "Would run:"
    echo "  gunzip -c $FILE | docker exec -i $MYSQL_CONTAINER mysql -uroot -p**** $DB_NAME"
    exit 0
fi

# ----- Confirmation -----
echo ""
echo "WARNING: this will overwrite ALL data in '$DB_NAME'."
read -rp 'Type "YES" to proceed: ' confirm
[[ "$confirm" == "YES" ]] || { echo "Aborted."; exit 1; }

# ----- Pre-restore safety backup -----
ts="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR/pre-restore"
safety="$BACKUP_DIR/pre-restore/agrios-pre-restore-${ts}.sql.gz"
echo ""
echo ">> Dumping current state to $safety ..."
docker exec "$MYSQL_CONTAINER" sh -c \
    "mysqldump -uroot -p\"$MYSQL_ROOT_PASSWORD\" --single-transaction --routines --triggers --events $DB_NAME" \
    | gzip > "$safety"
echo "   safety backup OK"

# ----- Restore -----
echo ""
echo ">> Restoring from $FILE ..."
gunzip -c "$FILE" | docker exec -i "$MYSQL_CONTAINER" \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME"

echo ""
echo ">> Restore complete. Safety backup kept at:"
echo "   $safety"
echo ""
echo ">> Verifying — table count + admin user:"
docker exec "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME" -e \
    "SELECT COUNT(*) AS tables FROM information_schema.tables WHERE table_schema='$DB_NAME'; SELECT id, username FROM sys_user WHERE username='admin';"
