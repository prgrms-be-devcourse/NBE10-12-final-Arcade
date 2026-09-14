#!/usr/bin/env bash
# webroot 방식으로 인증서를 갱신하고 Nginx 설정을 다시 읽힌다.

set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/arcade}"
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:v5.1.0}"
cd "$APP_DIR"

env_value() {
  sed -n "s/^$1=//p" .env | head -n1
}

resolve_path() {
  local value="$1" fallback="$2"
  [[ -n "$value" ]] || value="$fallback"
  if [[ "$value" = /* ]]; then
    printf '%s' "$value"
  else
    printf '%s/%s' "$APP_DIR" "${value#./}"
  fi
}

CERTBOT_CONF="$(resolve_path "$(env_value CERTBOT_CONF)" "./data/certbot/conf")"
CERTBOT_WEBROOT="$(resolve_path "$(env_value CERTBOT_WEBROOT)" "./data/certbot/www")"

docker run --rm \
  -v "$CERTBOT_CONF:/etc/letsencrypt" \
  -v "$CERTBOT_WEBROOT:/var/www/certbot" \
  "$CERTBOT_IMAGE" renew --webroot --webroot-path /var/www/certbot --quiet

compose_files=(-f docker-compose.yml)
[[ -f docker-compose.monitoring.yml ]] && compose_files+=(-f docker-compose.monitoring.yml)
docker compose "${compose_files[@]}" --env-file .env exec -T nginx nginx -s reload
