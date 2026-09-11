#!/usr/bin/env bash

set -euo pipefail

K6_IMAGE="grafana/k6:2.2.0"
ORIGIN=""
EXPECTED_VERSION=""
OLD_VERSION=""
MAX_DURATION="10m"
POST_DEPLOY_SECONDS="60"
RATE="5"
RESULTS_DIR="deploy-continuity-results"

while [ $# -gt 0 ]; do
  case "$1" in
    --origin)           ORIGIN="$2"; shift 2 ;;
    --expected-version) EXPECTED_VERSION="$2"; shift 2 ;;
    --old-version)      OLD_VERSION="$2"; shift 2 ;;
    --max-duration)     MAX_DURATION="$2"; shift 2 ;;
    --post-deploy-seconds) POST_DEPLOY_SECONDS="$2"; shift 2 ;;
    --rate)             RATE="$2"; shift 2 ;;
    --results-dir)      RESULTS_DIR="$2"; shift 2 ;;
    --)                 shift; break ;;
    *) echo "모르는 옵션: $1" >&2; exit 2 ;;
  esac
done

[ -n "$ORIGIN" ] || { echo "--origin 이 필요하다" >&2; exit 2; }
[ -n "$EXPECTED_VERSION" ] || { echo "--expected-version 이 필요하다" >&2; exit 2; }
[ -n "$OLD_VERSION" ] || { echo "--old-version 이 필요하다" >&2; exit 2; }
[[ "$POST_DEPLOY_SECONDS" =~ ^[0-9]+$ ]] \
  || { echo "--post-deploy-seconds 는 0 이상의 정수여야 한다" >&2; exit 2; }
[ $# -gt 0 ] || { echo "-- 뒤에 배포 명령이 필요하다" >&2; exit 2; }

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$RESULTS_DIR"
RESULTS_DIR="$(cd "$RESULTS_DIR" && pwd)"
CONTAINER_NAME="arcade-k6-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}-$$"
K6_PID=""

cleanup() {
  if [ -n "$K6_PID" ] && kill -0 "$K6_PID" 2>/dev/null; then
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
    wait "$K6_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "== k6 최대 ${MAX_DURATION}, ${RATE} RPS 연속 요청 시작 =="
docker pull "$K6_IMAGE" >/dev/null
docker run --rm --name "$CONTAINER_NAME" \
  --user "$(id -u):$(id -g)" \
  -p 127.0.0.1:6565:6565 \
  -e BASE_URL="$ORIGIN" \
  -e EXPECTED_VERSION="$EXPECTED_VERSION" \
  -e OLD_VERSION="$OLD_VERSION" \
  -e MAX_DURATION="$MAX_DURATION" \
  -e RATE="$RATE" \
  -v "$ROOT/tests/k6:/scripts:ro" \
  -v "$RESULTS_DIR:/results" \
  "$K6_IMAGE" run \
  --address 0.0.0.0:6565 \
  --summary-export /results/summary.json \
  --out json=/results/samples.json \
  /scripts/deploy-continuity.js >"$RESULTS_DIR/k6.log" 2>&1 &
K6_PID=$!

sleep 5
kill -0 "$K6_PID" 2>/dev/null || {
  echo "k6가 요청을 시작하지 못했다" >&2
  cat "$RESULTS_DIR/k6.log" >&2
  wait "$K6_PID" || true
  exit 1
}

set +e
"$@"
DEPLOY_STATUS=$?

echo "== 배포 완료 후 ${POST_DEPLOY_SECONDS}초 추가 관찰 =="
sleep "$POST_DEPLOY_SECONDS"

CONTROL_STATUS=0
if kill -0 "$K6_PID" 2>/dev/null; then
  curl --fail --silent --show-error \
    --request PATCH \
    --header 'Content-Type: application/json' \
    --data '{"data":{"type":"status","id":"default","attributes":{"stopped":true}}}' \
    http://127.0.0.1:6565/v1/status >/dev/null \
    || CONTROL_STATUS=$?
  if [ "$CONTROL_STATUS" -ne 0 ]; then
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
  fi
else
  echo "k6가 사후 관찰 종료 전에 끝났다" >&2
  CONTROL_STATUS=1
fi

wait "$K6_PID"
K6_STATUS=$?
set -e
K6_PID=""

THRESHOLD_STATUS=0
if [ ! -f "$RESULTS_DIR/summary.json" ] || ! jq -e '
  (.metrics.http_req_failed.value // 1) == 0 and
  (.metrics.checks.value // 0) == 1 and
  (.metrics.dropped_iterations.count // 1) == 0 and
  (.metrics.expected_version_responses.count // 0) > 0 and
  (.metrics.unexpected_version_responses.count // 1) == 0
' "$RESULTS_DIR/summary.json" >/dev/null; then
  THRESHOLD_STATUS=1
fi

{
  echo "DEPLOY_STATUS=$DEPLOY_STATUS"
  echo "K6_STATUS=$K6_STATUS"
  echo "CONTROL_STATUS=$CONTROL_STATUS"
  echo "THRESHOLD_STATUS=$THRESHOLD_STATUS"
  echo "EXPECTED_VERSION=$EXPECTED_VERSION"
  echo "OLD_VERSION=$OLD_VERSION"
} > "$RESULTS_DIR/status.env"

echo "== k6 마지막 결과 =="
cat "$RESULTS_DIR/k6.log"
echo "배포 종료 코드: $DEPLOY_STATUS"
echo "k6 종료 코드: $K6_STATUS"
echo "k6 제어 종료 코드: $CONTROL_STATUS"
echo "k6 threshold 판정 코드: $THRESHOLD_STATUS"

if [ "$K6_STATUS" -ne 0 ] && [ "$K6_STATUS" -ne 103 ]; then
  exit 1
fi
if [ "$DEPLOY_STATUS" -ne 0 ] || [ "$CONTROL_STATUS" -ne 0 ] || [ "$THRESHOLD_STATUS" -ne 0 ]; then
  exit 1
fi
