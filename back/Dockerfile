# syntax=docker/dockerfile:1

# ---------- 1. 빌드 ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

COPY src ./src
# TODO: 나중에 TEST 고려
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
 && cp build/libs/*.jar /workspace/app.jar

# ---------- 2. 레이어 추출 ----------
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /workspace/app.jar ./app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination ./layers

# ---------- 3. 런타임 ----------
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache tzdata \
 && addgroup -S app \
 && adduser -S app -G app

ENV TZ=Asia/Seoul \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

WORKDIR /app

# 변경 빈도가 낮은 것부터 복사한다.
COPY --from=extract --chown=app:app /extract/layers/dependencies/ ./
COPY --from=extract --chown=app:app /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=app:app /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=app:app /extract/layers/application/ ./

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
# TODO: multiplatform build