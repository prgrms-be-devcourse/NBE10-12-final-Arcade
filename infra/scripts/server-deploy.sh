#!/usr/bin/env bash
# shellcheck disable=SC2086 # Compose 파일 인자 문자열을 의도적으로 단어 분리한다.
# EC2에서 전달받은 .env로 컨테이너를 배포한다.

set -euo pipefail

PREFIX="${SSM_PREFIX:-/arcade/dev}"
REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="${APP_DIR:-/opt/arcade}"

cd "$APP_DIR"

# 외부에서 받은 .env는 SSM 값으로 덮어쓰지 않는다.
if [ "${ENV_FROM_SSM:-0}" != "1" ]; then
  echo "== .env 는 밖에서 받은 것을 쓴다 =="
  [ -s .env ] || { echo "  .env 가 없거나 비었다" >&2; exit 1; }
  echo "  $(grep -c . .env) 개 항목"
else

echo "== SSM 파라미터로 .env 생성 =="
# 파라미터 이름의 마지막 조각이 그대로 환경변수 이름이 된다.
umask 077
aws ssm get-parameters-by-path \
    --region "$REGION" --path "$PREFIX" --recursive --with-decryption \
    --query 'Parameters[*].[Name,Value]' --output text |
  while IFS="$(printf '\t')" read -r name value; do
    printf '%s=%s\n' "${name##*/}" "$value"
  done > .env
echo "  $(wc -l < .env) 개 항목"
fi

# 공백 포함 값을 실행하지 않도록 .env를 source하지 않는다.
env_value() {
  sed -n "s/^$1=//p" .env | head -n1
}

echo "== 레지스트리 로그인 =="
# <접두사>_USER 와 <접두사>_TOKEN 이 .env 에 있는 레지스트리에만 로그인한다.
# 없으면 그 레지스트리는 공개 이미지만 받는다 — 배포를 여기서 멈추지 않는다.
#
# GHCR 은 패키지가 비공개면 로그인이 필요하다. 저장소가 public 이어도
# 컨테이너 패키지는 따라가지 않는다 (기본이 private).
registry_login() {
  local label="$1" host="$2" user token out
  local -a args=()

  user="$(env_value "${label}_USER")"
  token="$(env_value "${label}_TOKEN")"

  if [ -z "$user" ] || [ -z "$token" ]; then
    echo "  ${label}_USER/_TOKEN 이 없다 — ${host:-docker.io} 는 공개 이미지만 받는다"
    return 0
  fi

  # 서버 인자를 빼면 Docker Hub 다. 빈 문자열로 넘기면 로그인이 깨진다.
  [ -n "$host" ] && args+=("$host")
  args+=(-u "$user" --password-stdin)

  # 토큰은 stdin 으로만 준다. 인자로 주면 ps 와 셸 이력에 남는다.
  if out="$(printf '%s' "$token" | docker login "${args[@]}" 2>&1)"; then
    echo "  ${host:-docker.io} — $user 로 로그인"
  else
    echo "  ${host:-docker.io} — 로그인 실패. 이 레지스트리 이미지는 못 받는다" >&2
    printf '    %s\n' "$out" >&2
  fi
}

if [ "${REGISTRY_AUTH_PRECONFIGURED:-0}" = "1" ]; then
  echo "  EICE 배포의 임시 Docker 인증 설정을 사용한다"
else
  registry_login DOCKERHUB ""
  registry_login GHCR ghcr.io
fi

# 모니터링 파일은 합치되 서비스는 프로필로 선택한다.
COMPOSE_FILES="-f docker-compose.yml"
[ -f docker-compose.monitoring.yml ] && COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.monitoring.yml"
echo "  compose 파일: $COMPOSE_FILES"

# 지표 인증 비밀번호는 SSM에서 읽고 서버에는 해시만 남긴다.
echo "== 지표 basic auth 파일 =="
MU="$(env_value METRICS_BASIC_AUTH_USER)"
MP="$(env_value METRICS_BASIC_AUTH_PASSWORD)"
# PGDATA 소유권을 보존하도록 필요한 경로만 sudo로 만든다.
sudo mkdir -p data/nginx
if [ -n "$MU" ] && [ -n "$MP" ]; then
  printf '%s:%s\n' "$MU" "$(openssl passwd -apr1 "$MP")" | sudo tee data/nginx/metrics.htpasswd >/dev/null
  echo "  $MU 로 생성"
else
  echo "  METRICS_BASIC_AUTH_* 가 없다. 빈 파일로 둔다 — 아무도 못 들어간다" >&2
  : | sudo tee data/nginx/metrics.htpasswd >/dev/null
fi
# nginx worker가 해시 파일을 읽을 수 있게 한다.
sudo chmod 644 data/nginx/metrics.htpasswd

echo "== 이미지 받기 =="
# SSM 출력 한도를 보존하도록 pull 진행률을 숨긴다.
docker compose $COMPOSE_FILES --env-file .env pull --quiet

echo "== 기반 서비스 기동 =="
# 트래픽을 받는 세 서비스는 아래에서 별도로 기동·전환한다.
mapfile -t OTHER_SERVICES < <(
  docker compose $COMPOSE_FILES --env-file .env config --services \
    | grep -Ev '^(backend|frontend|nginx)$'
)
echo "  대상: ${OTHER_SERVICES[*]}"
if [ "${#OTHER_SERVICES[@]}" -gt 0 ]; then
  # shellcheck disable=SC2086
  docker compose $COMPOSE_FILES --env-file .env up -d --no-build --quiet-pull \
    --remove-orphans --no-deps "${OTHER_SERVICES[@]}"
fi

# pg_dump에는 클러스터 전역 역할이 포함되지 않는다. 새 서버와 비밀번호 변경 배포에서
# exporter 역할을 생성·동기화한 뒤 수집기를 다시 연결한다.
if docker compose $COMPOSE_FILES --env-file .env config --services | grep -qx postgres-exporter; then
  echo "== PostgreSQL exporter 역할 동기화 =="
  APP_DIR="$APP_DIR" ./infra/scripts/server-init-postgres-exporter.sh
  # restart는 생성 당시 환경변수를 유지하므로 비밀번호 변경을 반영하려면 재생성해야 한다.
  docker compose $COMPOSE_FILES --env-file .env up -d --no-deps --force-recreate postgres-exporter >/dev/null
fi

# 변경된 애플리케이션만 준비한 뒤 Nginx 대상을 한 번에 전환한다.
NGINX_CONFIG="infra/nginx/nginx.prod.conf"
NGINX_TEMPLATE="$(mktemp)"
NGINX_RENDERED="$(mktemp)"
NGINX_PREVIOUS="$(mktemp)"
cp "$NGINX_CONFIG" "$NGINX_TEMPLATE"
trap 'rm -f "$NGINX_TEMPLATE" "$NGINX_RENDERED" "$NGINX_PREVIOUS"' EXIT

single_service_id() {
  local service="$1"
  local -a ids
  mapfile -t ids < <(
    docker compose $COMPOSE_FILES --env-file .env ps -q "$service" 2>/dev/null \
      | sed '/^$/d'
  )
  [ "${#ids[@]}" -le 1 ] || {
    echo "  $service 기존 컨테이너가 여러 개다: ${ids[*]}" >&2
    return 1
  }
  [ "${#ids[@]}" -eq 0 ] || printf '%s\n' "${ids[0]}"
}

container_name() {
  local name
  name="$(docker inspect "$1" --format '{{.Name}}' 2>/dev/null)"
  name="${name#/}"
  [[ "$name" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] || {
    echo "  안전하지 않은 컨테이너 이름: $name" >&2
    return 1
  }
  printf '%s\n' "$name"
}

service_needs_replacement() {
  local service="$1" current_id="$2"
  local desired_hash current_hash desired_ref desired_image current_image

  [ -n "$current_id" ] || return 0

  desired_hash="$(
    docker compose $COMPOSE_FILES --env-file .env config --hash "$service" \
      | awk -v service="$service" '$1 == service { print $2 }'
  )"
  current_hash="$(
    docker inspect "$current_id" \
      --format '{{index .Config.Labels "com.docker.compose.config-hash"}}' 2>/dev/null
  )"
  desired_ref="$(
    docker compose $COMPOSE_FILES --env-file .env config --format json \
      | jq -r --arg service "$service" '.services[$service].image // empty'
  )"
  desired_image="$(docker image inspect "$desired_ref" --format '{{.Id}}' 2>/dev/null)"
  current_image="$(docker inspect "$current_id" --format '{{.Image}}' 2>/dev/null)"

  if [ -n "$desired_hash" ] && [ "$desired_hash" = "$current_hash" ] \
    && [ -n "$desired_image" ] && [ "$desired_image" = "$current_image" ]; then
    return 1
  fi
  return 0
}

wait_container_healthy() {
  local service="$1" id="$2" max_seconds="$3" elapsed status
  for elapsed in $(seq 0 3 "$max_seconds"); do
    status="$(docker inspect "$id" --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' 2>/dev/null)"
    [ "$status" = healthy ] && { echo "  $service 준비됨 (${elapsed}초)"; return 0; }
    [ "$status" = unhealthy ] && break
    [ "$elapsed" -gt 0 ] && [ $((elapsed % 30)) -eq 0 ] \
      && echo "    $service ${elapsed}초 경과 — ${status:-조회 실패}"
    sleep 3
  done
  echo "  $service가 healthy가 안 됐다 (${status:-조회 실패})" >&2
  docker logs "$id" 2>&1 | tail -25 >&2
  return 1
}

CANDIDATE_ID=""
prepare_candidate() {
  local service="$1" old_id="$2" max_seconds="$3"
  local -a ids candidates
  candidates=()
  CANDIDATE_ID=""

  echo "  새 $service 기동"
  if [ -z "$old_id" ]; then
    # shellcheck disable=SC2086
    docker compose $COMPOSE_FILES --env-file .env up -d --no-build --no-deps "$service" >/dev/null
  else
    # shellcheck disable=SC2086
    docker compose $COMPOSE_FILES --env-file .env up -d --no-build --no-deps \
      --no-recreate --scale "$service=2" "$service" >/dev/null
  fi

  mapfile -t ids < <(
    docker compose $COMPOSE_FILES --env-file .env ps -q "$service" 2>/dev/null \
      | sed '/^$/d'
  )
  for id in "${ids[@]}"; do
    [ "$id" = "$old_id" ] || candidates+=("$id")
  done
  [ "${#candidates[@]}" -eq 1 ] || {
    echo "  새 $service 컨테이너를 하나로 식별하지 못했다: ${candidates[*]}" >&2
    return 1
  }

  CANDIDATE_ID="${candidates[0]}"
  wait_container_healthy "$service" "$CANDIDATE_ID" "$max_seconds"
}

render_nginx_config() {
  local backend_name="$1" frontend_name="$2" output="$3"
  sed -E \
      -e "s|[^[:space:];]+:8080; # ACTIVE_BACKEND|$backend_name:8080; # ACTIVE_BACKEND|g" \
      -e "s|[^[:space:];]+:3000; # ACTIVE_FRONTEND|$frontend_name:3000; # ACTIVE_FRONTEND|g" \
      "$NGINX_TEMPLATE" > "$output"
  [ "$(grep -Fc "$backend_name:8080; # ACTIVE_BACKEND" "$output")" -eq 4 ] \
    && [ "$(grep -Fc "$frontend_name:3000; # ACTIVE_FRONTEND" "$output")" -eq 2 ]
}

remove_candidate() {
  local new_id="$1" old_id="$2"
  [ -z "$new_id" ] || [ "$new_id" = "$old_id" ] \
    || docker rm -f "$new_id" >/dev/null 2>&1 || true
}

activate_nginx() {
  local backend_name="$1" frontend_name="$2" was_running=0
  docker compose $COMPOSE_FILES --env-file .env ps -q nginx 2>/dev/null | grep -q . \
    && was_running=1

  render_nginx_config "$backend_name" "$frontend_name" "$NGINX_RENDERED"
  # bind mount가 계속 같은 inode를 보도록 mv 대신 내용을 덮어쓴다.
  cat "$NGINX_RENDERED" > "$NGINX_CONFIG"

  if [ "$was_running" -eq 0 ]; then
    # shellcheck disable=SC2086
    docker compose $COMPOSE_FILES --env-file .env up -d --no-build --no-deps nginx >/dev/null
  fi

  docker compose $COMPOSE_FILES --env-file .env exec -T nginx \
    grep -Fq "$backend_name:8080" /etc/nginx/nginx.conf \
    && docker compose $COMPOSE_FILES --env-file .env exec -T nginx \
      grep -Fq "$frontend_name:3000" /etc/nginx/nginx.conf \
    && docker compose $COMPOSE_FILES --env-file .env exec -T nginx nginx -t \
    && { [ "$was_running" -eq 0 ] \
      || docker compose $COMPOSE_FILES --env-file .env exec -T nginx nginx -s reload; } \
    && return 0

  echo "  nginx 전환 실패 — 기존 설정과 컨테이너를 유지한다" >&2
  if [ -s "$NGINX_PREVIOUS" ]; then
    cat "$NGINX_PREVIOUS" > "$NGINX_CONFIG"
  fi
  [ "$was_running" -eq 1 ] \
    || docker compose $COMPOSE_FILES --env-file .env rm -sf nginx >/dev/null 2>&1 || true
  return 1
}

OLD_BACKEND_ID="$(single_service_id backend)"
OLD_FRONTEND_ID="$(single_service_id frontend)"
if { [ -n "$OLD_BACKEND_ID" ] && [ -z "$OLD_FRONTEND_ID" ]; } \
  || { [ -z "$OLD_BACKEND_ID" ] && [ -n "$OLD_FRONTEND_ID" ]; }; then
  echo "  backend와 frontend 중 하나만 실행 중이다. 상태를 먼저 정리한다" >&2
  exit 1
fi

if [ -n "$OLD_BACKEND_ID" ]; then
  OLD_BACKEND_NAME="$(container_name "$OLD_BACKEND_ID")"
  OLD_FRONTEND_NAME="$(container_name "$OLD_FRONTEND_ID")"
  render_nginx_config "$OLD_BACKEND_NAME" "$OLD_FRONTEND_NAME" "$NGINX_PREVIOUS"
  echo "== 기존 애플리케이션 대상으로 Nginx 고정 =="
  activate_nginx "$OLD_BACKEND_NAME" "$OLD_FRONTEND_NAME" || exit 1
fi

NEW_BACKEND_ID=""
NEW_FRONTEND_ID=""
BACKEND_CHANGED=1
FRONTEND_CHANGED=1
if [ "${ROLLING:-1}" = "1" ]; then
  echo "== 애플리케이션 무중단 교체 =="
  if service_needs_replacement backend "$OLD_BACKEND_ID"; then
    if ! prepare_candidate backend "$OLD_BACKEND_ID" 240; then
      remove_candidate "$CANDIDATE_ID" "$OLD_BACKEND_ID"
      exit 1
    fi
    NEW_BACKEND_ID="$CANDIDATE_ID"
  else
    BACKEND_CHANGED=0
    NEW_BACKEND_ID="$OLD_BACKEND_ID"
    echo "  backend 변경 없음 — 기존 컨테이너 유지"
  fi

  if service_needs_replacement frontend "$OLD_FRONTEND_ID"; then
    if ! prepare_candidate frontend "$OLD_FRONTEND_ID" 120; then
      remove_candidate "$CANDIDATE_ID" "$OLD_FRONTEND_ID"
      remove_candidate "$NEW_BACKEND_ID" "$OLD_BACKEND_ID"
      exit 1
    fi
    NEW_FRONTEND_ID="$CANDIDATE_ID"
  else
    FRONTEND_CHANGED=0
    NEW_FRONTEND_ID="$OLD_FRONTEND_ID"
    echo "  frontend 변경 없음 — 기존 컨테이너 유지"
  fi
else
  echo "== 애플리케이션 교체 (중단 허용) =="
  # shellcheck disable=SC2086
  docker compose $COMPOSE_FILES --env-file .env up -d --no-build backend frontend
  NEW_BACKEND_ID="$(single_service_id backend)"
  NEW_FRONTEND_ID="$(single_service_id frontend)"
  wait_container_healthy backend "$NEW_BACKEND_ID" 240
  wait_container_healthy frontend "$NEW_FRONTEND_ID" 120
fi

NEW_BACKEND_NAME="$(container_name "$NEW_BACKEND_ID")"
NEW_FRONTEND_NAME="$(container_name "$NEW_FRONTEND_ID")"
echo "  활성 대상: backend=$NEW_BACKEND_NAME frontend=$NEW_FRONTEND_NAME"
if { [ "$BACKEND_CHANGED" -eq 1 ] || [ "$FRONTEND_CHANGED" -eq 1 ]; } \
  && ! activate_nginx "$NEW_BACKEND_NAME" "$NEW_FRONTEND_NAME"; then
  remove_candidate "$NEW_FRONTEND_ID" "$OLD_FRONTEND_ID"
  remove_candidate "$NEW_BACKEND_ID" "$OLD_BACKEND_ID"
  exit 1
fi

if [ -n "$OLD_BACKEND_ID" ] && [ "${ROLLING:-1}" = "1" ] \
  && { [ "$BACKEND_CHANGED" -eq 1 ] || [ "$FRONTEND_CHANGED" -eq 1 ]; }; then
  echo "  기존 Nginx worker 요청 drain (16초)"
  sleep 16
  OLD_IDS=()
  [ "$FRONTEND_CHANGED" -eq 0 ] || OLD_IDS+=("$OLD_FRONTEND_ID")
  [ "$BACKEND_CHANGED" -eq 0 ] || OLD_IDS+=("$OLD_BACKEND_ID")
  docker stop "${OLD_IDS[@]}" >/dev/null
  docker rm "${OLD_IDS[@]}" >/dev/null
fi
echo "  애플리케이션 교체 완료"

echo "== 설정을 파일에서 읽는 서비스 갱신 =="
RUNNING=$(docker compose $COMPOSE_FILES --env-file .env ps --services --status running 2>/dev/null)

for svc in caddy prometheus grafana; do
  if printf '%s\n' "$RUNNING" | grep -qx "$svc"; then
    if docker compose $COMPOSE_FILES --env-file .env restart "$svc" >/dev/null 2>&1; then
      echo "  $svc 재시작"
    else
      echo "  $svc 재시작 실패" >&2
      exit 1
    fi
  fi
done

wait_healthy() {
  local svc="$1" max_seconds="$2" id status elapsed
  id=$(docker compose $COMPOSE_FILES --env-file .env ps -q "$svc" 2>/dev/null | head -1)
  [ -n "$id" ] || { echo "  $svc 컨테이너를 찾지 못했다" >&2; return 1; }

  status=$(docker inspect "$id" --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' 2>/dev/null)
  if [ "$status" = none ]; then
    echo "  $svc healthcheck 없음 — 대기 생략"
    return 0
  fi

  for elapsed in $(seq 0 3 "$max_seconds"); do
    status=$(docker inspect "$id" --format '{{.State.Health.Status}}' 2>/dev/null)
    [ "$status" = healthy ] && { echo "  $svc 정상 ($elapsed초)"; return 0; }
    [ "$status" = unhealthy ] && break
    sleep 3
  done

  echo "  $svc가 정상화되지 않았다 ($status)" >&2
  docker logs "$id" 2>&1 | tail -25 >&2
  return 1
}

echo "== 모니터링 정상화 확인 =="
for svc in prometheus grafana; do
  if printf '%s\n' "$RUNNING" | grep -qx "$svc"; then
    wait_healthy "$svc" 120 || exit 1
  fi
done

# 재생성한 인스턴스에도 적용되도록 배포 때 systemd 백업 타이머를 등록한다.
echo "== 백업 타이머 등록 =="
sudo tee /etc/systemd/system/arcade-backup.service >/dev/null <<UNIT
[Unit]
Description=Arcade DB dump to local EBS
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
User=ec2-user
WorkingDirectory=$APP_DIR
Environment=BACKUP_RETENTION_DAYS=7
ExecStart=$APP_DIR/infra/scripts/server-backup.sh
UNIT

sudo tee /etc/systemd/system/arcade-backup.timer >/dev/null <<UNIT
[Unit]
Description=Arcade DB dump daily

[Timer]
OnCalendar=daily
# 놓친 실행은 다음 부팅 때 따라잡는다.
Persistent=true
RandomizedDelaySec=15m

[Install]
WantedBy=timers.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable --now arcade-backup.timer >/dev/null 2>&1
echo "  $(systemctl is-enabled arcade-backup.timer 2>/dev/null) / 다음 실행: $(systemctl show arcade-backup.timer -p NextElapseUSecRealtime --value 2>/dev/null)"

echo
docker compose $COMPOSE_FILES --env-file .env ps
