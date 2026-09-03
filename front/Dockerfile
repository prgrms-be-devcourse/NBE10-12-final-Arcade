# syntax=docker/dockerfile:1

ARG NODE_VERSION=22-alpine
# CI(.github/workflows/ci.yaml)의 pnpm 메이저와 맞춘다.
ARG PNPM_VERSION=11.25.0

# ---------- 0. 공통 베이스 ----------
FROM node:${NODE_VERSION} AS base
ARG PNPM_VERSION
RUN npm install -g pnpm@${PNPM_VERSION}
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /app

# ---------- 1. 의존성 ----------
FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/pnpm/store \
    pnpm install --frozen-lockfile --store-dir /pnpm/store

# ---------- 2. 빌드 ----------
FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 외부 공개 주소는 한 번만 받고, 브라우저 API 주소는 이미지 안에서 조립한다.
#   docker build --build-arg PUBLIC_ORIGIN=https://app.example.com ...
ARG PUBLIC_ORIGIN
ARG NEXT_PUBLIC_USE_MOCK=""
ENV NEXT_PUBLIC_API_BASE_URL=${PUBLIC_ORIGIN}/api/v1 \
    NEXT_PUBLIC_USE_MOCK=${NEXT_PUBLIC_USE_MOCK}

RUN test -n "$PUBLIC_ORIGIN" \
 && pnpm exec next typegen \
 && pnpm build

# ---------- 3. 런타임 ----------
FROM base AS runner

RUN apk add --no-cache tzdata
ENV NODE_ENV=production \
    TZ=Asia/Seoul \
    PORT=3000 \
    HOSTNAME=0.0.0.0

# 서버(SSR)가 부르는 주소. NEXT_PUBLIC_ 이 아니라서 번들에 박히지 않고 런타임에 읽힌다.
# 컨테이너 안에서 localhost 는 자기 자신이라 브라우저용 주소를 그대로 쓸 수 없다.
#   docker run -e API_BASE_URL_INTERNAL=http://backend:8080/api/v1 ...
# 비어 있으면 NEXT_PUBLIC_API_BASE_URL 로 떨어진다. 기본값은 그 동작을 유지한다.
ENV API_BASE_URL_INTERNAL=""

COPY --from=builder --chown=node:node /app/package.json ./package.json
COPY --from=builder --chown=node:node /app/next.config.ts ./next.config.ts
COPY --from=builder --chown=node:node /app/public ./public
COPY --from=builder --chown=node:node /app/.next ./.next
COPY --from=builder --chown=node:node /app/node_modules ./node_modules

# node:alpine 이미지에 이미 들어있는 비루트 계정을 쓴다.
USER node
EXPOSE 3000

# health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/ > /dev/null || exit 1

# pnpm 을 거치지 않고 직접 실행해 SIGTERM 이 next 서버까지 그대로 전달되게 한다.
CMD ["./node_modules/.bin/next", "start"]
