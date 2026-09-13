#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  echo "사용법: $0 [env-file] [backup-directory]" >&2
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
  usage
  exit 0
fi
if (($# > 2)); then
  usage
  exit 1
fi

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-${PROJECT_DIR}/.env.prod}"
BACKUP_DIR="${2:-${PROJECT_DIR}/backups/postgres}"

[[ -r "$ENV_FILE" ]] || {
  echo "환경변수 파일을 읽을 수 없습니다: $ENV_FILE" >&2
  exit 1
}
command -v docker >/dev/null || {
  echo "docker 명령을 찾을 수 없습니다." >&2
  exit 1
}

compose=(docker compose --project-directory "$PROJECT_DIR" --env-file "$ENV_FILE" -f "$PROJECT_DIR/docker-compose.yml")
"${compose[@]}" config --quiet
"${compose[@]}" up -d postgres

for _ in {1..30}; do
  if "${compose[@]}" exec -T postgres sh -ceu 'exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
"${compose[@]}" exec -T postgres sh -ceu 'exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null

umask 077
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

timestamp="$(date '+%Y%m%dT%H%M%S%z')"
backup_file="${BACKUP_DIR}/postgres-${timestamp}.dump"
[[ ! -e "$backup_file" ]] || {
  echo "같은 이름의 백업이 이미 존재합니다: $backup_file" >&2
  exit 1
}
temp_file="$(mktemp "${BACKUP_DIR}/.postgres-backup.XXXXXX")"
trap 'rm -f -- "$temp_file"' EXIT

"${compose[@]}" exec -T postgres sh -ceu '
  export PGPASSWORD="$POSTGRES_PASSWORD"
  exec pg_dump \
    --host=127.0.0.1 \
    --username="$POSTGRES_USER" \
    --dbname="$POSTGRES_DB" \
    --format=custom \
    --no-owner \
    --no-privileges
' >"$temp_file"

[[ -s "$temp_file" ]] || {
  echo "빈 dump가 생성되었습니다." >&2
  exit 1
}
"${compose[@]}" exec -T postgres pg_restore --list <"$temp_file" >/dev/null

chmod 600 "$temp_file"
mv -- "$temp_file" "$backup_file"
trap - EXIT

echo "PostgreSQL 백업 완료: $backup_file"
