#!/usr/bin/env bash

set -euo pipefail

K6_IMAGE="grafana/k6:2.2.0"
ORIGIN=""
EXPECTED_VERSION=""
OLD_VERSION=""
DURATION="7m"
RATE="5"
RESULTS_DIR="deploy-continuity-results"

while [ $# -gt 0 ]; do
  case "$1" in
    --origin)           ORIGIN="$2"; shift 2 ;;
    --expected-version) EXPECTED_VERSION="$2"; shift 2 ;;
    --old-version)      OLD_VERSION="$2"; shift 2 ;;
    --duration)         DURATION="$2"; shift 2 ;;
    --rate)             RATE="$2"; shift 2 ;;
    --results-dir)      RESULTS_DIR="$2"; shift 2 ;;
    --)                 shift; break ;;
    *) echo "모르는 옵션: $1" >&2; exit 2 ;;
  esac
done

[ -n "$ORIGIN" ] || { echo "--origin 이 필요하다" >&2; exit 2; }
[ -n "$EXPECTED_VERSION" ] || { echo "--expected-version 이 필요하다" >&2; exit 2; }
[ -n "$OLD_VERSION" ] || { echo "--old-version 이 필요하다" >&2; exit 2; }
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

echo "== k6 ${DURATION}, ${RATE} RPS 연속 요청 시작 =="
docker pull "$K6_IMAGE" >/dev/null
docker run --rm --name "$CONTAINER_NAME" \
  --user "$(id -u):$(id -g)" \
  -e BASE_URL="$ORIGIN" \
  -e EXPECTED_VERSION="$EXPECTED_VERSION" \
  -e OLD_VERSION="$OLD_VERSION" \
  -e DURATION="$DURATION" \
  -e RATE="$RATE" \
  -v "$ROOT/tests/k6:/scripts:ro" \
  -v "$RESULTS_DIR:/results" \
  "$K6_IMAGE" run \
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
wait "$K6_PID"
K6_STATUS=$?
set -e
K6_PID=""

{
  echo "DEPLOY_STATUS=$DEPLOY_STATUS"
  echo "K6_STATUS=$K6_STATUS"
  echo "EXPECTED_VERSION=$EXPECTED_VERSION"
  echo "OLD_VERSION=$OLD_VERSION"
} > "$RESULTS_DIR/status.env"

echo "== k6 마지막 결과 =="
cat "$RESULTS_DIR/k6.log"
echo "배포 종료 코드: $DEPLOY_STATUS"
echo "k6 종료 코드: $K6_STATUS"

if [ "$DEPLOY_STATUS" -ne 0 ] || [ "$K6_STATUS" -ne 0 ]; then
  exit 1
fi
