#!/usr/bin/env bash
# EC2에서 SSM 설정으로 컨테이너를 배포한다.

set -euo pipefail

PREFIX="${SSM_PREFIX:-/arcade/dev}"
REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="${APP_DIR:-/opt/arcade}"

cd "$APP_DIR"

# 외부에서 받은 .env는 SSM 값으로 덮어쓰지 않는다.
if [ "${ENV_FROM_SSM:-1}" != "1" ]; then
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

registry_login DOCKERHUB ""
registry_login GHCR ghcr.io

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

echo "== 기동 =="
# 고아 서비스를 제거하되 backend는 아래에서 별도로 롤링한다.
OTHERS="$(docker compose $COMPOSE_FILES --env-file .env config --services | grep -vx backend | tr '\n' ' ')"
echo "  대상: $OTHERS"
# shellcheck disable=SC2086
docker compose $COMPOSE_FILES --env-file .env up -d --no-build --quiet-pull \
  --remove-orphans --no-deps $OTHERS

# 새 backend가 healthy일 때만 기존 컨테이너를 제거한다.
rolling_backend() {
  local old_id new_id i st
  old_id="$(docker compose $COMPOSE_FILES --env-file .env ps -q backend 2>/dev/null | head -1)"

  if [ -z "$old_id" ]; then
    echo "  기존 백엔드가 없다 (첫 기동). 그냥 띄운다"
    docker compose $COMPOSE_FILES --env-file .env up -d --no-build backend
    return 0
  fi

  echo "  새 백엔드를 옆에 띄운다"
  if ! docker compose $COMPOSE_FILES --env-file .env up -d --no-build --no-deps \
       --no-recreate --scale backend=2 backend >/dev/null 2>&1; then
    echo "  scale 실패 — 옛것을 유지한 채 중단한다" >&2
    return 1
  fi

  new_id="$(docker compose $COMPOSE_FILES --env-file .env ps -q backend | grep -vx "$old_id" | head -1)"
  if [ -z "$new_id" ]; then
    echo "  새 컨테이너를 못 찾았다 — 이미지가 그대로면 겹치지 않는다" >&2
    return 1
  fi

  # 두 backend가 CPU를 나눠 쓰므로 넉넉히 기다린다.
  echo "  healthy 대기 (최대 240초)"
  for i in $(seq 1 80); do
    st="$(docker inspect "$new_id" --format '{{.State.Health.Status}}' 2>/dev/null)"
    [ "$st" = healthy ] && { echo "  준비됨 ($((i * 3))초)"; break; }
    [ "$st" = unhealthy ] && { echo "  unhealthy 판정 ($((i * 3))초)"; break; }
    [ $((i % 10)) -eq 0 ] && echo "    $((i * 3))초 경과 — $st"
    sleep 3
  done
  if [ "$st" != healthy ]; then
    echo "  새 백엔드가 healthy 가 안 됐다($st)" >&2
    echo "  --- 새 컨테이너 로그 (마지막 25줄) ---" >&2
    docker logs "$new_id" 2>&1 | tail -25 >&2
    echo "  --- 헬스체크 마지막 결과 ---" >&2
    docker inspect "$new_id" --format '{{range .State.Health.Log}}{{.ExitCode}} {{.Output}}{{end}}' 2>/dev/null | tail -3 >&2
    echo "  새것을 지우고 옛것을 남긴다" >&2
    docker rm -f "$new_id" >/dev/null 2>&1
    return 1
  fi

  echo "  옛 백엔드를 내린다 (stop_grace_period 만큼 기다린다)"
  docker stop "$old_id" >/dev/null && docker rm "$old_id" >/dev/null
  echo "  교체 완료"
}

if [ "${ROLLING:-1}" = "1" ]; then
  echo "== 백엔드 무중단 교체 =="
  rolling_backend || echo "  롤링 실패. 백엔드는 옛 버전 그대로다" >&2
else
  echo "== 백엔드 교체 (중단 허용) =="
  docker compose $COMPOSE_FILES --env-file .env up -d --no-build backend
fi

# bind mount 설정 변경은 Compose가 감지하지 못하므로 서비스를 갱신한다.
echo "== 설정을 파일에서 읽는 서비스 갱신 =="
RUNNING=$(docker compose $COMPOSE_FILES --env-file .env ps --services --status running 2>/dev/null)

# 진행 중인 연결을 보존하도록 nginx는 reload한다.
if printf '%s\n' "$RUNNING" | grep -qx nginx; then
  if docker compose $COMPOSE_FILES --env-file .env exec -T nginx nginx -s reload >/dev/null 2>&1; then
    echo "  nginx reload"
  else
    echo "  nginx reload 실패 — 재시작으로 대체한다" >&2
    docker compose $COMPOSE_FILES --env-file .env restart nginx >/dev/null 2>&1 \
      && echo "  nginx 재시작" || echo "  nginx 재시작도 실패" >&2
  fi
fi

for svc in caddy prometheus grafana; do
  if printf '%s\n' "$RUNNING" | grep -qx "$svc"; then
    docker compose $COMPOSE_FILES --env-file .env restart "$svc" >/dev/null 2>&1 \
      && echo "  $svc 재시작" || echo "  $svc 재시작 실패"
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
