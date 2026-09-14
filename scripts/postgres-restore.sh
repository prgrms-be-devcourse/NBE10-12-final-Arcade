#!/usr/bin/env bash
# shellcheck disable=SC2016 # 단일 인용 문자열은 컨테이너 내부 셸에서 확장한다.
set -Eeuo pipefail

usage() {
  echo "사용법: $0 <dump-file> [env-file]" >&2
  echo "비대화식 실행: RESTORE_CONFIRM=<database-name> $0 <dump-file> [env-file]" >&2
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
  usage
  exit 0
fi
if (($# < 1 || $# > 2)); then
  usage
  exit 1
fi

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DUMP_FILE="$1"
ENV_FILE="${2:-${PROJECT_DIR}/.env.prod}"

[[ -s "$DUMP_FILE" && -r "$DUMP_FILE" ]] || {
  echo "dump 파일을 읽을 수 없거나 비어 있습니다: $DUMP_FILE" >&2
  exit 1
}
[[ -r "$ENV_FILE" ]] || {
  echo "환경변수 파일을 읽을 수 없습니다: $ENV_FILE" >&2
  exit 1
}
command -v docker >/dev/null || {
  echo "docker 명령을 찾을 수 없습니다." >&2
  exit 1
}

compose_files=(-f "$PROJECT_DIR/docker-compose.yml")
if [[ -f "$PROJECT_DIR/docker-compose.monitoring.yml" ]]; then
  compose_files+=(-f "$PROJECT_DIR/docker-compose.monitoring.yml")
fi
compose=(docker compose --project-directory "$PROJECT_DIR" --env-file "$ENV_FILE" "${compose_files[@]}")
"${compose[@]}" config --quiet
"${compose[@]}" up -d postgres

for _ in {1..30}; do
  if "${compose[@]}" exec -T postgres sh -ceu 'exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
"${compose[@]}" exec -T postgres sh -ceu 'exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null
"${compose[@]}" exec -T postgres pg_restore --list <"$DUMP_FILE" >/dev/null

database_name="$("${compose[@]}" exec -T postgres sh -ceu 'printf %s "$POSTGRES_DB"')"
[[ "$database_name" != "postgres" && "$database_name" != template0 && "$database_name" != template1 ]] || {
  echo "시스템 데이터베이스에는 복원할 수 없습니다: $database_name" >&2
  exit 1
}

confirmation="${RESTORE_CONFIRM:-}"
if [[ -z "$confirmation" && -t 0 ]]; then
  echo "대상 데이터베이스 '$database_name'을 삭제하고 복원합니다." >&2
  read -r -p "계속하려면 데이터베이스 이름을 입력하세요: " confirmation
fi
[[ "$confirmation" == "$database_name" ]] || {
  echo "확인값이 일치하지 않아 복원을 중단합니다." >&2
  usage
  exit 1
}

running_services="$("${compose[@]}" ps --status running --services)"
restart_services=()
if grep -qx backend <<<"$running_services"; then
  restart_services+=(backend)
fi
if grep -qx postgres-exporter <<<"$running_services"; then
  restart_services+=(postgres-exporter)
fi
if ((${#restart_services[@]} > 0)); then
  "${compose[@]}" stop "${restart_services[@]}"
fi

restore_complete=false
trap 'if [[ $restore_complete != true ]]; then echo "복원 실패: 애플리케이션 서비스를 중지한 상태로 유지합니다." >&2; fi' EXIT

"${compose[@]}" exec -T postgres sh -ceu '
  export PGPASSWORD="$POSTGRES_PASSWORD"
  dropdb \
    --host=127.0.0.1 \
    --username="$POSTGRES_USER" \
    --maintenance-db=postgres \
    --force \
    --if-exists \
    "$POSTGRES_DB"
  createdb \
    --host=127.0.0.1 \
    --username="$POSTGRES_USER" \
    --owner="$POSTGRES_USER" \
    "$POSTGRES_DB"
'

"${compose[@]}" exec -T postgres sh -ceu '
  export PGPASSWORD="$POSTGRES_PASSWORD"
  exec pg_restore \
    --host=127.0.0.1 \
    --username="$POSTGRES_USER" \
    --dbname="$POSTGRES_DB" \
    --no-owner \
    --no-privileges \
    --exit-on-error
' <"$DUMP_FILE"

"${compose[@]}" exec -T postgres sh -ceu '
  export PGPASSWORD="$POSTGRES_PASSWORD"
  exec psql --host=127.0.0.1 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --tuples-only --command="SELECT 1"
' | grep -q 1

if ((${#restart_services[@]} > 0)); then
  "${compose[@]}" up -d "${restart_services[@]}"
fi
restore_complete=true
trap - EXIT

echo "PostgreSQL 복원 완료: $DUMP_FILE -> $database_name"
