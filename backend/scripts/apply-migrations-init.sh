#!/bin/bash
# ============================================================================
# MySQL docker-entrypoint-initdb.d hook — auto-apply all AgriOS migrations.
#
# Mounted at /docker-entrypoint-initdb.d/02-apply-migrations.sh, this script
# runs on the FIRST boot of a fresh MySQL data volume (the standard docker
# entrypoint contract). It walks every .sql file under /migrations/ in
# numeric order and applies each one to the AgriOS database.
#
# Each migration file is idempotent (CREATE TABLE IF NOT EXISTS / INSERT
# IGNORE / ON DUPLICATE KEY UPDATE), so re-runs during development or after
# `docker compose down -v` are safe. mysql --force keeps going on per-statement
# errors so one stray failure (e.g. an ALTER that's already been applied via
# schema.sql) doesn't abort the whole sequence.
#
# Used together with the existing /docker-entrypoint-initdb.d/01-schema.sql
# mount, the boot order becomes:
#   01-schema.sql      → foundation tables + constraints + indexes
#   02-apply-...sh     → this script, runs 009 through 044 in numeric order
#
# After this, the freshly booted MySQL is fully ready for the AgriOS backend
# to connect without any "Table doesn't exist" errors.
# ============================================================================
set -uo pipefail

MIGRATIONS_DIR=/migrations
DB="${MYSQL_DATABASE:?MYSQL_DATABASE must be set}"
ROOT_PASS="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set}"

if [ ! -d "$MIGRATIONS_DIR" ]; then
  echo "[apply-migrations] $MIGRATIONS_DIR not mounted, skipping."
  exit 0
fi

echo "[apply-migrations] applying $(ls "$MIGRATIONS_DIR"/*.sql 2>/dev/null | wc -l) migration(s) to $DB"

for f in $(ls "$MIGRATIONS_DIR"/*.sql 2>/dev/null | sort); do
  name=$(basename "$f")
  echo "[apply-migrations] -> $name"
  if ! mysql --force -uroot -p"$ROOT_PASS" "$DB" < "$f" 2>&1 | grep -v "^mysql: \[Warning\]" >&2 ; then
    echo "[apply-migrations] !! $name had errors (continuing — they are usually 'already exists' on re-runs)"
  fi
done

echo "[apply-migrations] done."
