# 로컬 모니터링 스택

Prometheus, Grafana, Caddy를 애플리케이션 Compose와 분리해 실행한다. 현재 구성은 로컬 확인용이며 Grafana 익명 접근이 열려 있으므로 공개 환경에 그대로 배포하면 안 된다. cAdvisor는 기본 Compose에 포함하지 않고 아래의 선택적 구성으로만 남겨 두었다.

## 실행

프로젝트 루트에서 기본 스택을 올린다.

```bash
docker compose -f infra/monitoring/docker-compose.yml up -d
```

Prometheus 설정을 재시작 없이 반영하려면 lifecycle API를 호출한다.

```bash
curl -X POST http://localhost:9090/-/reload
```

## 접근 주소

| 서비스     | Caddy 진입점                       | 직접 주소               | 비고                                               |
| ---------- | ---------------------------------- | ----------------------- | -------------------------------------------------- |
| Grafana    | `http://grafana.localhost:8081`    | `http://localhost:3001` | 호스트명 없이 Caddy에 접속해도 Grafana로 연결된다. |
| Prometheus | `http://prometheus.localhost:8081` | `http://localhost:9090` | 백엔드와 Prometheus 자체를 수집한다.               |

Caddy 포트는 `MONITORING_HTTP_PORT`로 바꿀 수 있으며 TLS는 사용하지 않는다.

## 대시보드

- [JVM (Micrometer)](grafana/dashboards/jvm-micrometer.md): JVM·프로세스·HTTP 지표를 심층 분석한다.
- [Spring Boot Statistics](grafana/dashboards/spring-boot-statistics.md): JVM·HikariCP·HTTP·Logback 상태를 운영 관점에서 확인한다.

## Prometheus 수집

`arcade-backend` job은 `host.docker.internal:8080/actuator/prometheus`을 수집한다. 애플리케이션과 모니터링이 서로 다른 Compose 프로젝트이므로 Docker Desktop의 호스트 주소를 사용한다. 두 스택을 같은 Docker 네트워크에 넣으면 `backend:8080`과 같은 서비스 이름으로 변경한다.

Grafana 커뮤니티 대시보드가 인스턴스를 선택할 수 있도록 `application: arcade-backend` 라벨을 수집 시점에 붙인다. 애플리케이션이 직접 같은 라벨을 내보내게 하려면 Spring의 `management.metrics.tags.application`을 사용한다.

호스트 CPU·메모리·디스크 지표는 현재 구성에 없다.

Docker API에서 컨테이너 이름을 읽기 위해 `/var/run/docker.sock`을 명시적으로 마운트한다.

현재 `pid: host`와 `--containerd=/proc/1/root/run/containerd/containerd.sock`는 Docker Desktop for macOS의 VM 안 containerd socket에 접근하기 위한 설정이다. 일반 Linux Docker 호스트에서는 해당 `pid`와 command 인자를 제거해야 할 수 있다.

## Grafana provisioning

- Prometheus 데이터소스는 기동 시 `http://prometheus:9090`으로 자동 등록된다.
- 대시보드 JSON이 참조하는 `prometheus` UID를 고정한다. UID 없이 만들어진 기존 데이터소스가 남아 있을 수 있으므로 같은 이름을 지운 뒤 재생성한다.
- Provisioning 대시보드는 UI에서 수정해도 저장되지 않는다. `grafana/dashboards` 아래 JSON을 수정하면 30초 주기로 재로드된다.
- UI에서 대시보드를 지워도 다음 스캔에 복구된다. 영구적으로 제거하려면 JSON 파일을 제거한다.
- 대시보드 마운트 경로 `/etc/grafana/dashboards`는 provider의 `options.path`와 같아야 한다.

Grafana 관리자 기본 계정은 로컬 확인용 `admin`/`admin`이며 익명 Viewer 접근도 허용한다. 외부 환경에서는 계정·비밀번호를 변경하고 익명 접근을 끄며 인증과 TLS를 구성한다.
