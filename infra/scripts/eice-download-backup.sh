#!/usr/bin/env bash
# EIC 임시 키와 EICE 터널로 EC2의 PostgreSQL 백업을 내려받는다.
#
#   ./infra/scripts/eice-download-backup.sh --create
#   ./infra/scripts/eice-download-backup.sh <instance-id> --create --output-dir backups/postgres
#
# --create는 서버의 arcade-backup.service를 먼저 실행한 뒤 새로 생성된 최신 dump를 받는다.

set -euo pipefail
umask 077

REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="/opt/arcade"
NAME_TAG="arcade-ec2"
OUTPUT_DIR="backups"
INSTANCE=""
REQUESTED="latest"
CREATE_BACKUP=0

while [ $# -gt 0 ]; do
  case "$1" in
    --region)     REGION="$2"; shift 2 ;;
    --name)       NAME_TAG="$2"; shift 2 ;;
    --dir)        APP_DIR="$2"; shift 2 ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    --file)       REQUESTED="$2"; shift 2 ;;
    --create)     CREATE_BACKUP=1; shift ;;
    -h|--help)
      sed -n '2,/^set -euo pipefail$/p' "$0" | sed '$d'
      exit 0
      ;;
    -*) echo "모르는 옵션: $1" >&2; exit 1 ;;
    *)  INSTANCE="$1"; shift ;;
  esac
done

[[ "$APP_DIR" =~ ^/[A-Za-z0-9._/-]+$ ]] || { echo "안전하지 않은 서버 경로: $APP_DIR" >&2; exit 1; }
if [ "$REQUESTED" != "latest" ]; then
  [[ "$REQUESTED" =~ ^arcade-[0-9]{8}-[0-9]{6}\.dump$ ]] || {
    echo "--file은 arcade-YYYYMMDD-HHMMSS.dump 형식이어야 한다" >&2
    exit 1
  }
fi
[ "$CREATE_BACKUP" -eq 0 ] || [ "$REQUESTED" = "latest" ] || {
  echo "--create와 --file은 함께 사용할 수 없다" >&2
  exit 1
}

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
trap 'rm -rf "$WORK_DIR"' EXIT
ssh-keygen -q -t ed25519 -N '' -f "$WORK_DIR/eic-key"

PROXY_COMMAND="aws ec2-instance-connect open-tunnel --region $REGION --instance-id %h --instance-connect-endpoint-id $EICE_ID --remote-port 22 --max-tunnel-duration 1200"
SSH_OPTIONS=(
  -i "$WORK_DIR/eic-key"
  -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=accept-new
  -o "UserKnownHostsFile=$WORK_DIR/known_hosts"
  -o "ProxyCommand=$PROXY_COMMAND"
)

push_key() {
  aws ec2-instance-connect send-ssh-public-key --region "$REGION" \
    --instance-id "$INSTANCE" --availability-zone "$AZ" \
    --instance-os-user ec2-user --ssh-public-key "file://$WORK_DIR/eic-key.pub" >/dev/null
}

remote_command() {
  push_key
  ssh "${SSH_OPTIONS[@]}" "ec2-user@$INSTANCE" "$1"
}

if [ "$CREATE_BACKUP" -eq 1 ]; then
  echo "== $INSTANCE PostgreSQL 백업 생성 중 =="
  remote_command "sudo systemctl start arcade-backup.service"
fi

if [ "$REQUESTED" = "latest" ]; then
  REQUESTED=$(remote_command \
    "find '$APP_DIR/backups' -maxdepth 1 -type f -name 'arcade-*.dump' -printf '%T@ %f\\n' | sort -nr | head -n1 | cut -d' ' -f2-")
  [ -n "$REQUESTED" ] || { echo "서버에 백업이 없다" >&2; exit 1; }
fi
[[ "$REQUESTED" =~ ^arcade-[0-9]{8}-[0-9]{6}\.dump$ ]] || {
  echo "서버가 안전하지 않은 백업 파일명을 반환했다: $REQUESTED" >&2
  exit 1
}

mkdir -p "$OUTPUT_DIR"
LOCAL_FILE="$OUTPUT_DIR/$REQUESTED"
[ ! -e "$LOCAL_FILE" ] || { echo "이미 파일이 있다: $LOCAL_FILE" >&2; exit 1; }

echo "== $INSTANCE:$APP_DIR/backups/$REQUESTED 내려받는 중 =="
REMOTE_SHA=$(remote_command \
  "sha256sum '$APP_DIR/backups/$REQUESTED' | cut -d' ' -f1")
push_key
scp -q "${SSH_OPTIONS[@]}" \
  "ec2-user@$INSTANCE:$APP_DIR/backups/$REQUESTED" "$WORK_DIR/$REQUESTED"

LOCAL_SIZE=$(wc -c < "$WORK_DIR/$REQUESTED")
[ "$LOCAL_SIZE" -gt 1024 ] || { echo "받은 덤프가 너무 작다" >&2; exit 1; }
if command -v sha256sum >/dev/null 2>&1; then
  LOCAL_SHA=$(sha256sum "$WORK_DIR/$REQUESTED" | cut -d' ' -f1)
else
  LOCAL_SHA=$(shasum -a 256 "$WORK_DIR/$REQUESTED" | cut -d' ' -f1)
fi
[ "$LOCAL_SHA" = "$REMOTE_SHA" ] || { echo "SHA-256 검증에 실패했다" >&2; exit 1; }

install -m 600 "$WORK_DIR/$REQUESTED" "$LOCAL_FILE"
echo "완료: $LOCAL_FILE ($LOCAL_SIZE 바이트, sha256=$LOCAL_SHA)"
