# 모니터링 스택

Prometheus 와 Grafana. 실행 방법이 **둘**이고, 목적이 다르다.

| 방법 | 파일 | 언제 |
| --- | --- | --- |
| **앱과 함께** | 루트의 `docker-compose.monitoring.yml` | 서버 배포. 앱 컨테이너를 실제로 관측할 때 |
| **단독** | `local/docker-compose.yml` | 로컬에서 모니터링 스택만 확인할 때 |
| **운영 조회** | `remote/docker-compose.yml` | 각 팀원의 로컬 Grafana에서 서버 Prometheus를 함께 볼 때 |

`prometheus/`와 `grafana/`는 로컬·서버 모니터링용 원본이다. 운영 Prometheus를 조회하는
팀원용 Grafana는 `remote/grafana/`에 대시보드와 provisioning을 따로 둔다. 운영 조회에
필요한 필터 변경이 기존 로컬 대시보드에 섞이지 않게 하기 위해서다.

## 앱과 함께 (기본)

루트 compose 와 **같은 프로젝트**로 합쳐 쓴다. 진입점 Caddy 하나가 앱과 Grafana 를 함께
받으려면 네트워크를 공유해야 하고, 그러려면 같은 compose 프로젝트여야 한다.

```bash
COMPOSE_PROFILES=local,monitoring,grafana \
CADDYFILE=./infra/caddy/Caddyfile.monitoring \
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml --env-file .env up -d
```

프로필이 둘로 갈려 있다. **수집과 열람을 따로 켠다.**

| `COMPOSE_PROFILES` | 뜨는 것 |
| --- | --- |
| `local,monitoring` | 앱 + Prometheus (수집만) |
| `local,monitoring,grafana` | 열람까지 |

Grafana 가 Prometheus 보다 훨씬 무겁다(실측 264~505MB 대 42~56MB). 수집은 상시 돌아야
하지만 — 꺼진 동안의 지표는 영영 없다 — 열람은 볼 때만 있으면 된다.

포트로 가른다. 앱은 `:80`, 모니터링은 `:8081`.

## 단독 실행 (로컬 전용)

```bash
docker compose -f infra/monitoring/local/docker-compose.yml up -d
```

앱과 무관하게 모니터링만 띄워 볼 때 쓴다. **이 방식으로는 백엔드 지표를 못 받는다** —
`prometheus.yml` 의 수집 대상이 `backend:8080` 인데 같은 네트워크가 아니라서 그 이름을
찾지 못한다.

## 접근 주소

| | 앱과 함께 | 단독 |
| --- | --- | --- |
| Grafana | `http://<호스트>:8081` | `http://grafana.localhost:8081` 또는 `http://localhost:3001` |
| Prometheus | `http://<호스트>:8081/prometheus` | `http://prometheus.localhost:8081` 또는 `http://localhost:9090` |

Caddy 포트는 `MONITORING_HTTP_PORT` 로 바꾼다. TLS 는 쓰지 않는다.

## 팀원과 운영 대시보드 공유

Grafana 자체를 작은 운영 서버에 추가하지 않고, 각 팀원이 같은 프로비저닝 설정으로 로컬
Grafana를 띄워 `metrics.crewon.cloud`의 Prometheus를 조회한다. 대시보드 JSON은 저장소로
공유되고 조회 계정만 별도로 전달한다.

```bash
cp infra/monitoring/remote/.env.example infra/monitoring/remote/.env
# .env에서 Grafana 비밀번호와 Prometheus Basic Auth 계정만 채운다.
docker compose -p arcade-monitoring \
  -f infra/monitoring/remote/docker-compose.yml \
  --env-file infra/monitoring/remote/.env up -d --force-recreate grafana
```

브라우저에서 `http://localhost:3001`로 접속한다. 운영 애플리케이션의 `.env.prod` 전체를
팀원에게 복사하지 않는다. `remote/.env`에는 모니터링 조회에 필요한 값만 둔다.

Prometheus 설정을 재시작 없이 반영하려면 lifecycle API 를 호출한다.

```bash
curl -X POST http://localhost:9090/-/reload
```

## 대시보드

- [JVM (Micrometer)](grafana/dashboards/jvm-micrometer.md) — JVM·프로세스·HTTP 지표 심층 분석
- [Spring Boot Statistics](grafana/dashboards/spring-boot-statistics.md) — JVM·HikariCP·HTTP·Logback 운영 관점
- 운영 조회 전용 `Nginx — Arcade Dev` — 처리량과 연결 상태
- 운영 조회 전용 `PostgreSQL — Arcade Dev` — 연결·트랜잭션·캐시·deadlock

## Prometheus 수집

`arcade-backend` job 이 `backend:8080/actuator/prometheus` 를 수집한다.

운영 부하테스트에서는 다음 exporter도 같은 `monitoring` 프로필로 실행한다.

- `nginx-exporter:9113`: Nginx up/down, 총 요청률, active/reading/writing/waiting 연결
- `postgres-exporter:9187`: DB 연결, 트랜잭션, buffer cache, tuple, deadlock, DB 크기

Nginx OSS의 `stub_status`에는 HTTP 상태 코드별 지표가 없다. 따라서 `429` 개수는 부하
발생기 결과에서 확인한다. Nginx 자체에서 상태 코드별 시계열이 필요하면 access log
exporter 또는 Loki를 별도로 도입해야 한다.

보존 기간은 `PROMETHEUS_RETENTION`(기본 7일)으로 정한다. 시계열이 쌓이는 만큼 메모리도
늘어나므로 작은 인스턴스에서는 짧게 잡는다.

호스트 CPU·메모리·디스크 지표는 현재 구성에 없다. `node_exporter` 는 미도입이다.

## Grafana provisioning

- Prometheus 데이터소스는 기동 시 `http://prometheus:9090` 으로 자동 등록된다
- 대시보드 JSON 이 참조하는 `prometheus` UID 를 고정한다. UID 없이 만들어진 기존
  데이터소스가 남아 있을 수 있으므로 같은 이름을 지운 뒤 재생성한다
- Provisioning 대시보드는 UI 에서 수정해도 저장되지 않는다. `grafana/dashboards` 아래
  JSON 을 고치면 30초 주기로 재로드된다
- UI 에서 지워도 다음 스캔에 복구된다. 영구히 없애려면 JSON 파일을 지운다
- 마운트 경로 `/etc/grafana/dashboards` 는 provider 의 `options.path` 와 같아야 한다

## 보안

기본 계정은 로컬 확인용 `admin`/`admin` 이고 익명 Viewer 접근도 열려 있다.
**서버에서는 반드시 바꾼다.**

```
GF_SECURITY_ADMIN_PASSWORD=<시크릿>
GF_AUTH_ANONYMOUS_ENABLED=false
```

Prometheus 는 인증이 없다. 포트를 직접 열지 말고 Caddy 뒤에 두거나 SSH·SSM 터널로 붙는다.
