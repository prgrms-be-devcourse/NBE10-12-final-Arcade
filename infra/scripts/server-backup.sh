#!/usr/bin/env bash
# EC2에서 PostgreSQL 논리 백업을 만들고 EBS에 7일간 보관한다.

set -euo pipefail
umask 077

# systemd 환경에서도 docker를 찾을 수 있게 한다.
export PATH="/usr/local/bin:/usr/bin:/bin:$PATH"

APP_DIR="${APP_DIR:-/opt/arcade}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

cd "$APP_DIR"

[ -f .env ] || { echo ".env 가 없다. EICE 배포를 먼저 실행한다" >&2; exit 1; }
[[ "$RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]] || {
  echo "BACKUP_RETENTION_DAYS는 1 이상의 정수여야 한다" >&2
  exit 1
}

env_value() {
  local key="$1"
  sed -n "s/^${key}=//p" .env | head -n1
}

DB_NAME="$(env_value POSTGRES_DB)"
DB_USER="$(env_value POSTGRES_USER)"
DB_PASS="$(env_value POSTGRES_PASSWORD)"
[ -n "$DB_NAME" ] || { echo ".env의 POSTGRES_DB가 비었다" >&2; exit 1; }
[ -n "$DB_USER" ] || { echo ".env의 POSTGRES_USER가 비었다" >&2; exit 1; }
[ -n "$DB_PASS" ] || { echo ".env의 POSTGRES_PASSWORD가 비었다" >&2; exit 1; }

STAMP="$(date -u +%Y%m%d-%H%M%S)"
OUT="$APP_DIR/backups/arcade-$STAMP.dump"
mkdir -p "$APP_DIR/backups"
chmod 700 "$APP_DIR/backups"

# Compose 필수 변수 해석을 위해 .env를 명시한다.
CID="$(docker compose -f docker-compose.yml --env-file .env ps -q postgres)"
[ -n "$CID" ] || { echo "postgres 컨테이너가 없다. 서버가 기동 중인지 확인한다" >&2; exit 1; }

echo "== 덤프 -> $OUT =="
# 이식 가능한 PostgreSQL 커스텀 포맷을 쓴다.
docker exec -e PGPASSWORD="$DB_PASS" "$CID" \
  pg_dump -U "$DB_USER" -d "$DB_NAME" -Fc --no-owner --no-privileges > "$OUT"
SIZE=$(wc -c < "$OUT")
echo "  $SIZE 바이트"

# 비정상적으로 작은 덤프는 보관하지 않는다.
[ "$SIZE" -gt 1024 ] || { echo "덤프가 너무 작다. 보관하지 않는다" >&2; rm -f "$OUT"; exit 1; }
chmod 600 "$OUT"

echo "== ${RETENTION_DAYS}일보다 오래된 로컬 백업 정리 =="
find "$APP_DIR/backups" -type f -name 'arcade-*.dump' \
  -mmin "+$((RETENTION_DAYS * 1440))" -print -delete
