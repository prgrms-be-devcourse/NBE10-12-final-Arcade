#!/usr/bin/env bash
# 새 서버에서 Nginx를 시작하기 전에 Let's Encrypt 인증서를 최초 발급한다.

set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/arcade}"
CERTBOT_EMAIL="${1:-}"
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:v5.1.0}"

if [[ "$CERTBOT_EMAIL" == "-h" || "$CERTBOT_EMAIL" == "--help" ]]; then
  echo "사용법: $0 <certbot-email>"
  exit 0
fi
[[ "$CERTBOT_EMAIL" == *@* ]] || {
  echo "사용법: $0 <certbot-email>" >&2
  exit 1
}

cd "$APP_DIR"
[[ -s .env ]] || { echo ".env가 없거나 비었다" >&2; exit 1; }

env_value() {
  sed -n "s/^$1=//p" .env | head -n1
}

resolve_path() {
  case "$1" in
    /*) printf '%s' "$1" ;;
    *) printf '%s/%s' "$APP_DIR" "${1#./}" ;;
  esac
}

PUBLIC_ORIGIN="$(env_value PUBLIC_ORIGIN)"
DOMAIN="${PUBLIC_ORIGIN#https://}"
DOMAIN="${DOMAIN#http://}"
DOMAIN="${DOMAIN%%/*}"
DOMAIN="${DOMAIN%%:*}"
[[ "$DOMAIN" =~ ^[A-Za-z0-9.-]+$ ]] || {
  echo "PUBLIC_ORIGIN에서 유효한 도메인을 찾지 못했다: $PUBLIC_ORIGIN" >&2
  exit 1
}
METRICS_DOMAIN="$(env_value METRICS_DOMAIN)"
[ -n "$METRICS_DOMAIN" ] || METRICS_DOMAIN="metrics.$DOMAIN"
[[ "$METRICS_DOMAIN" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]$ ]] || {
  echo "유효하지 않은 METRICS_DOMAIN: $METRICS_DOMAIN" >&2
  exit 1
}

CERTBOT_CONF="$(resolve_path "$(env_value CERTBOT_CONF || true)")"
CERTBOT_WEBROOT="$(resolve_path "$(env_value CERTBOT_WEBROOT || true)")"
[[ "$CERTBOT_CONF" != "$APP_DIR/" ]] || CERTBOT_CONF="$APP_DIR/data/certbot/conf"
[[ "$CERTBOT_WEBROOT" != "$APP_DIR/" ]] || CERTBOT_WEBROOT="$APP_DIR/data/certbot/www"

sudo install -d -m 0755 "$CERTBOT_CONF" "$CERTBOT_WEBROOT"

CERTIFICATE="$CERTBOT_CONF/live/$DOMAIN/fullchain.pem"
PRIVATE_KEY="$CERTBOT_CONF/live/$DOMAIN/privkey.pem"
if sudo test -s "$CERTIFICATE" &&
   sudo test -s "$PRIVATE_KEY" &&
   sudo openssl x509 -checkhost "$DOMAIN" -noout -in "$CERTIFICATE" >/dev/null &&
   sudo openssl x509 -checkhost "$METRICS_DOMAIN" -noout -in "$CERTIFICATE" >/dev/null; then
  echo "== Certbot 인증서가 이미 있다: $DOMAIN, $METRICS_DOMAIN =="
else
  compose_files=(-f docker-compose.yml)
  [[ -f docker-compose.monitoring.yml ]] && compose_files+=(-f docker-compose.monitoring.yml)
  docker compose "${compose_files[@]}" --env-file .env stop nginx >/dev/null 2>&1 || true

  domains=(-d "$DOMAIN" -d "$METRICS_DOMAIN")

  echo "== Certbot 최초 발급: ${domains[*]} =="
  docker run --rm --name arcade-certbot-init \
    -p 80:80 \
    -v "$CERTBOT_CONF:/etc/letsencrypt" \
    "$CERTBOT_IMAGE" certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --cert-name "$DOMAIN" \
    --expand \
    --email "$CERTBOT_EMAIL" \
    "${domains[@]}"

  sudo test -s "$CERTIFICATE"
  sudo test -s "$PRIVATE_KEY"
  echo "== Certbot 최초 발급 완료: $DOMAIN, $METRICS_DOMAIN =="
fi

echo "== Certbot 자동 갱신 타이머 등록 =="
sudo tee /etc/systemd/system/arcade-certbot-renew.service >/dev/null <<UNIT
[Unit]
Description=Renew Arcade Let's Encrypt certificates
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
User=ec2-user
WorkingDirectory=$APP_DIR
Environment=APP_DIR=$APP_DIR
Environment=CERTBOT_IMAGE=$CERTBOT_IMAGE
ExecStart=$APP_DIR/infra/scripts/server-renew-certbot.sh
UNIT

sudo tee /etc/systemd/system/arcade-certbot-renew.timer >/dev/null <<'UNIT'
[Unit]
Description=Check Arcade Let's Encrypt certificates twice daily

[Timer]
OnCalendar=*-*-* 00,12:00:00
Persistent=true
RandomizedDelaySec=30m

[Install]
WantedBy=timers.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable --now arcade-certbot-renew.timer >/dev/null
