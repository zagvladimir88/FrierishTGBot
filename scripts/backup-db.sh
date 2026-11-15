#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
target="${BACKUP_DIR:-/opt/fraerish-bot-backup/postgres}"
keep_days="${BACKUP_KEEP_DAYS:-14}"

mkdir -p "$target"
file="$target/fraerish-$(date +%F).sql.gz"
docker compose exec -T postgres pg_dump -U fraerish -d fraerish | gzip > "$file.tmp"
mv "$file.tmp" "$file"
find "$target" -name 'fraerish-*.sql.gz' -mtime +"$keep_days" -delete
echo "$(date -Is) backup written to $file"