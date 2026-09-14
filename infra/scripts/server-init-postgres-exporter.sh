#!/usr/bin/env bash
# EC2에서 postgres-exporter 전용 DB 역할을 현재 Compose 환경과 동기화한다.

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/arcade}"
cd "$APP_DIR"

COMPOSE_ARGS=(-f docker-compose.yml)
[ -f docker-compose.monitoring.yml ] && COMPOSE_ARGS+=(-f docker-compose.monitoring.yml)

compose_environment=$(docker compose "${COMPOSE_ARGS[@]}" --env-file .env config --environment)
compose_env_value() {
  printf '%s\n' "$compose_environment" | sed -n "s/^$1=//p" | head -n1
}

exporter_user=$(compose_env_value POSTGRES_EXPORTER_USER)
exporter_user="${exporter_user:-postgres_exporter}"
exporter_password=$(compose_env_value POSTGRES_EXPORTER_PASSWORD)

[[ "$exporter_user" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] \
  || { echo "안전하지 않은 POSTGRES_EXPORTER_USER: $exporter_user" >&2; exit 1; }
[ -n "$exporter_password" ] \
  || { echo "POSTGRES_EXPORTER_PASSWORD가 없다" >&2; exit 1; }

postgres_id=$(docker compose "${COMPOSE_ARGS[@]}" --env-file .env ps -q postgres 2>/dev/null | head -1)
[ -n "$postgres_id" ] || { echo "postgres 컨테이너를 찾지 못했다" >&2; exit 1; }

echo "  postgres healthy 대기"
for _ in $(seq 0 3 60); do
  status=$(docker inspect "$postgres_id" --format '{{.State.Health.Status}}' 2>/dev/null)
  [ "$status" = healthy ] && break
  [ "$status" = unhealthy ] && { echo "postgres가 unhealthy다" >&2; exit 1; }
  sleep 3
done
[ "$status" = healthy ] || { echo "postgres healthy 대기 시간 초과" >&2; exit 1; }

container_env_value() {
  docker inspect "$postgres_id" --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | sed -n "s/^$1=//p" | head -n1
}

postgres_user=$(container_env_value POSTGRES_USER)
postgres_db=$(container_env_value POSTGRES_DB)
[ -n "$postgres_user" ] && [ -n "$postgres_db" ] \
  || { echo "postgres 컨테이너의 DB 이름 또는 관리 계정을 읽지 못했다" >&2; exit 1; }

role_exists=$(docker exec "$postgres_id" psql -U "$postgres_user" -d "$postgres_db" -At \
  -c "SELECT 1 FROM pg_roles WHERE rolname = '$exporter_user'" | head -n1)
if [ "$role_exists" != 1 ]; then
  docker exec "$postgres_id" psql -U "$postgres_user" -d "$postgres_db" -v ON_ERROR_STOP=1 \
    -c "CREATE ROLE \"$exporter_user\" LOGIN"
  echo "  $exporter_user 역할 생성"
fi

# 컨테이너 내부 localhost는 pg_hba.conf에 따라 비밀번호 없이 통과할 수 있어
# 로그인 시험으로 일치 여부를 판정하지 않고 매번 원하는 값으로 동기화한다.
printf '%s\n%s\n' "$exporter_password" "$exporter_password" \
  | docker exec -i "$postgres_id" psql -U "$postgres_user" -d "$postgres_db" \
    -v ON_ERROR_STOP=1 -c "\\password $exporter_user"
echo "  $exporter_user 비밀번호 동기화"

docker exec "$postgres_id" psql -U "$postgres_user" -d "$postgres_db" -v ON_ERROR_STOP=1 \
  -c "GRANT pg_monitor TO \"$exporter_user\"" >/dev/null

postgres_host=$(docker exec "$postgres_id" hostname -i | awk '{print $1}')
[ -n "$postgres_host" ] || { echo "postgres 컨테이너 IP를 찾지 못했다" >&2; exit 1; }
docker exec -e PGPASSWORD="$exporter_password" "$postgres_id" \
  psql -h "$postgres_host" -U "$exporter_user" -d "$postgres_db" -Atqc 'SELECT 1' \
  | grep -qx 1
echo "  $exporter_user 네트워크 로그인·pg_monitor 권한 정상"
