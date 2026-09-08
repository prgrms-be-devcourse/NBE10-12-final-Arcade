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

# 브라우저용 주소. 번들에 박히므로 주소가 바뀌면 이미지를 다시 만들어야 한다.
#   docker build --build-arg PUBLIC_ORIGIN=https://app.example.com ...
ARG PUBLIC_ORIGIN
ARG NEXT_PUBLIC_USE_MOCK=""
ENV NEXT_PUBLIC_API_BASE_URL=${PUBLIC_ORIGIN}/api/v1 \
    NEXT_PUBLIC_USE_MOCK=${NEXT_PUBLIC_USE_MOCK}

RUN test -n "$PUBLIC_ORIGIN" \
 && pnpm exec next typegen \
 && pnpm build

# ---------- 3. 런타임 ----------
FROM node:${NODE_VERSION} AS runner

RUN apk add --no-cache tzdata
ENV NODE_ENV=production \
    TZ=Asia/Seoul \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    NEXT_TELEMETRY_DISABLED=1

# SSR 이 부르는 주소. 컨테이너 안에서 localhost 는 자기 자신이라 브라우저용 주소를
# 그대로 못 쓴다. 비우면 NEXT_PUBLIC_API_BASE_URL 로 떨어진다.
ENV API_BASE_URL_INTERNAL=""

WORKDIR /app

COPY --from=builder --chown=node:node /app/.next/standalone ./
# standalone 은 정적 파일을 안 담는다. 빠뜨리면 페이지는 200 인데
# CSS·폰트·이미지만 404 가 난다 — 빌드가 아니라 런타임에 드러난다.
COPY --from=builder --chown=node:node /app/.next/static ./.next/static
COPY --from=builder --chown=node:node /app/public ./public

USER node
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/healthz > /dev/null || exit 1

# next CLI 를 거치지 않아야 SIGTERM 이 서버까지 그대로 전달된다.
CMD ["node", "server.js"]
