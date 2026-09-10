#!/usr/bin/env bash
# EIC 임시 키와 EICE 터널로 배포 파일을 전송하고 서버 배포 스크립트를 실행한다.
# GHCR_TOKEN은 인자가 아니라 환경변수로만 받는다.

set -euo pipefail

REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="/opt/arcade"
INSTANCE=""
NAME_TAG="arcade-dev"
ENV_FILE=""
BACKEND_IMAGE=""
FRONTEND_IMAGE=""
FILES_ONLY=0

while [ $# -gt 0 ]; do
  case "$1" in
    --region)         REGION="$2"; shift 2 ;;
    --name)           NAME_TAG="$2"; shift 2 ;;
    --dir)            APP_DIR="$2"; shift 2 ;;
    --env-file)       ENV_FILE="$2"; shift 2 ;;
    --backend-image)  BACKEND_IMAGE="$2"; shift 2 ;;
    --frontend-image) FRONTEND_IMAGE="$2"; shift 2 ;;
    --files-only)     FILES_ONLY=1; shift ;;
    -h|--help)
      sed -n '2,/^set -euo pipefail$/p' "$0" | sed '$d'
      exit 0
      ;;
    -*) echo "모르는 옵션: $1" >&2; exit 1 ;;
    *)  INSTANCE="$1"; shift ;;
  esac
done

[ -n "$ENV_FILE" ] || { echo "--env-file 이 필요하다" >&2; exit 1; }
[ -s "$ENV_FILE" ] || { echo "환경 파일이 없거나 비었다: $ENV_FILE" >&2; exit 1; }
[ -n "${GHCR_USER:-}" ] || { echo "GHCR_USER 가 필요하다" >&2; exit 1; }
[ -n "${GHCR_TOKEN:-}" ] || { echo "GHCR_TOKEN 이 필요하다" >&2; exit 1; }
[[ "$APP_DIR" =~ ^/[A-Za-z0-9._/-]+$ ]] || { echo "안전하지 않은 배포 경로: $APP_DIR" >&2; exit 1; }

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

FILES=(
  docker-compose.yml
  docker-compose.monitoring.yml
  infra/caddy
  infra/nginx/nginx.prod.conf
  infra/monitoring/prometheus
  infra/monitoring/grafana
  infra/scripts/server-deploy.sh
  infra/scripts/server-backup.sh
)

for file in "${FILES[@]}"; do
  [ -e "$file" ] || { echo "배포 파일이 없다: $file" >&2; exit 1; }
done

if [ -z "$INSTANCE" ]; then
  echo "== Name=$NAME_TAG 인스턴스 찾는 중 =="
  INSTANCE=$(aws ec2 describe-instances --region "$REGION" \
    --filters "Name=tag:Name,Values=$NAME_TAG" "Name=instance-state-name,Values=running" \
    --query 'Reservations[].Instances[].InstanceId' --output text)
  case "$INSTANCE" in
    ""|None) echo "실행 중인 인스턴스가 없다 (Name=$NAME_TAG)" >&2; exit 1 ;;
    *[[:space:]]*) echo "여러 인스턴스가 잡혔다: $INSTANCE" >&2; exit 1 ;;
  esac
fi

read -r AZ VPC_ID <<<"$(aws ec2 describe-instances --region "$REGION" \
  --instance-ids "$INSTANCE" \
  --query 'Reservations[0].Instances[0].[Placement.AvailabilityZone,VpcId]' --output text)"
[ -n "$AZ" ] && [ "$AZ" != "None" ] || { echo "인스턴스 가용 영역을 못 찾았다" >&2; exit 1; }

EICE_ID=$(aws ec2 describe-instance-connect-endpoints --region "$REGION" \
  --filters "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=create-complete" \
  --query 'InstanceConnectEndpoints[].InstanceConnectEndpointId' --output text)
case "$EICE_ID" in
  ""|None) echo "사용 가능한 EICE가 없다 (VPC=$VPC_ID)" >&2; exit 1 ;;
  *[[:space:]]*) echo "EICE가 여러 개다: $EICE_ID" >&2; exit 1 ;;
esac

WORK_DIR="$(mktemp -d)"
METADATA_DIR="$WORK_DIR/_deploy"
REMOTE_BUNDLE="/tmp/arcade-deploy-${GITHUB_RUN_ID:-$$}.tgz"
REMOTE_BUNDLE_UPLOADED=0
cleanup() {
  if [ "$REMOTE_BUNDLE_UPLOADED" = "1" ]; then
    push_key >/dev/null 2>&1 && \
      ssh "${SSH_OPTIONS[@]}" "ec2-user@$INSTANCE" "rm -f -- '$REMOTE_BUNDLE'" >/dev/null 2>&1 || true
  fi
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

mkdir -m 700 "$METADATA_DIR"
install -m 600 "$ENV_FILE" "$METADATA_DIR/env"
printf '%s' "$GHCR_USER" > "$METADATA_DIR/ghcr-user"
printf '%s' "$GHCR_TOKEN" > "$METADATA_DIR/ghcr-token"
printf '%s' "$BACKEND_IMAGE" > "$METADATA_DIR/backend-image"
printf '%s' "$FRONTEND_IMAGE" > "$METADATA_DIR/frontend-image"
chmod 600 "$METADATA_DIR"/*

echo "== 배포 묶음 생성 =="
COPYFILE_DISABLE=1 tar --no-xattrs -czf "$WORK_DIR/deploy.tgz" \
  "${FILES[@]}" -C "$WORK_DIR" _deploy
chmod 600 "$WORK_DIR/deploy.tgz"

ssh-keygen -q -t ed25519 -N '' -f "$WORK_DIR/eic-key"
KNOWN_HOSTS="$WORK_DIR/known_hosts"
PROXY_COMMAND="aws ec2-instance-connect open-tunnel --region $REGION --instance-id %h --instance-connect-endpoint-id $EICE_ID --remote-port 22 --max-tunnel-duration 1200"
SSH_OPTIONS=(
  -i "$WORK_DIR/eic-key"
  -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=accept-new
  -o "UserKnownHostsFile=$KNOWN_HOSTS"
  -o "ProxyCommand=$PROXY_COMMAND"
)

push_key() {
  aws ec2-instance-connect send-ssh-public-key --region "$REGION" \
    --instance-id "$INSTANCE" --availability-zone "$AZ" \
    --instance-os-user ec2-user --ssh-public-key "file://$WORK_DIR/eic-key.pub" >/dev/null
}

echo "== EICE로 파일 전송 =="
push_key
scp -q "${SSH_OPTIONS[@]}" "$WORK_DIR/deploy.tgz" "ec2-user@$INSTANCE:$REMOTE_BUNDLE"
REMOTE_BUNDLE_UPLOADED=1

echo "== 서버 배포 실행 =="
push_key
ssh "${SSH_OPTIONS[@]}" "ec2-user@$INSTANCE" \
  "bash -s -- '$APP_DIR' '$REMOTE_BUNDLE' '$REGION' '$FILES_ONLY'" <<'REMOTE_SCRIPT'
set -euo pipefail

APP_DIR="$1"
BUNDLE="$2"
REGION="$3"
FILES_ONLY="$4"
chmod 600 "$BUNDLE"
STAGE="$(mktemp -d /tmp/arcade-stage.XXXXXX)"
DOCKER_CONFIG="$STAGE/docker-config"
export DOCKER_CONFIG

cleanup() {
  local status=$?
  docker logout ghcr.io >/dev/null 2>&1 || true
  if rm -rf -- "$STAGE" "$BUNDLE"; then
    echo "== 서버 임시 인증정보 정리 완료 =="
  else
    echo "서버 임시 인증정보 정리 실패: $STAGE" >&2
    [ "$status" -ne 0 ] || status=1
  fi
  exit "$status"
}
trap cleanup EXIT

mkdir -m 700 "$DOCKER_CONFIG"
tar xzf "$BUNDLE" -C "$STAGE"
install -d "$APP_DIR"
tar --exclude='_deploy' -xzf "$BUNDLE" -C "$APP_DIR"
find "$APP_DIR/infra" -name '._*' -delete
chmod +x "$APP_DIR"/infra/scripts/*.sh

NEXT_ENV="$STAGE/.env.next"
install -m 600 "$STAGE/_deploy/env" "$NEXT_ENV"
# 마지막 줄에 개행이 없는 Secret이어도 뒤에 붙는 이미지 키와 합쳐지지 않게 한다.
printf '\n' >> "$NEXT_ENV"

env_value() {
  local file="$1" key="$2"
  [ -f "$file" ] || return 0
  sed -n "s/^${key}=//p" "$file" | head -n1
}

set_env() {
  local key="$1" value="$2" next="$NEXT_ENV.tmp"
  sed "/^${key}=/d" "$NEXT_ENV" > "$next"
  printf '%s=%s\n' "$key" "$value" >> "$next"
  mv "$next" "$NEXT_ENV"
  chmod 600 "$NEXT_ENV"
}

# 레지스트리 자격증명은 애플리케이션 환경 파일에 남기지 않는다.
sed -e '/^GHCR_USER=/d' -e '/^GHCR_TOKEN=/d' "$NEXT_ENV" > "$NEXT_ENV.clean"
mv "$NEXT_ENV.clean" "$NEXT_ENV"
chmod 600 "$NEXT_ENV"

for key in BACKEND_IMAGE FRONTEND_IMAGE; do
  file_name=$(printf '%s' "$key" | tr '[:upper:]_' '[:lower:]-')
  requested=$(cat "$STAGE/_deploy/$file_name")
  if [ -n "$requested" ]; then
    set_env "$key" "$requested"
  else
    current=$(env_value "$APP_DIR/.env" "$key")
    [ -n "$current" ] && set_env "$key" "$current"
  fi
  value=$(env_value "$NEXT_ENV" "$key")
  [ -n "$value" ] && [ "$value" != "NEED_TO_SET" ] || {
    echo "$key 값이 없다. 첫 배포에서는 이미지 두 개를 모두 지정한다" >&2
    exit 1
  }
done

printf '%s' "$(cat "$STAGE/_deploy/ghcr-token")" | docker login ghcr.io \
  -u "$(cat "$STAGE/_deploy/ghcr-user")" --password-stdin >/dev/null
chmod 600 "$DOCKER_CONFIG/config.json"

install -m 600 "$NEXT_ENV" "$APP_DIR/.env"
if [ "$FILES_ONLY" = "1" ]; then
  echo "파일만 전송했다. 배포는 건너뛴다."
else
  cd "$APP_DIR"
  AWS_REGION="$REGION" APP_DIR="$APP_DIR" ENV_FROM_SSM=0 REGISTRY_AUTH_PRECONFIGURED=1 \
    ./infra/scripts/server-deploy.sh
fi
REMOTE_SCRIPT
REMOTE_BUNDLE_UPLOADED=0
