# Spring Boot Statistics 대시보드

`spring-boot-statistics.json`은 Arcade 백엔드의 JVM, HikariCP, HTTP, Logback 상태를 운영 관점에서 보는 대시보드다. Grafana Labs 대시보드 10280을 Arcade 수집 라벨과 지표에 맞게 조정했으며 Grafana UID는 `arcade-spring-boot`다.

## 변수

| 변수 | 의미 |
| --- | --- |
| `application` | `jvm_info`의 `application` 라벨로 애플리케이션을 선택한다. |
| `instance` | 선택한 애플리케이션의 Prometheus 타깃을 선택한다. |
| `hikaricp` | 확인할 HikariCP pool을 선택한다. |
| `memory_pool_heap` | 확인할 heap 메모리 풀을 선택한다. |
| `memory_pool_nonheap` | 확인할 non-heap 메모리 풀을 선택한다. |

## Basic Statistics

| 패널 | 보여주는 값 | 확인 포인트 |
| --- | --- | --- |
| `Uptime`, `Start time` | 프로세스 기동 시각과 실행 시간 | 배포 외의 재시작이 있었는지 확인한다. |
| `Heap Used`, `Non-Heap Used` | used/max 메모리 비율 | 장기 상승 패턴을 본다. max가 없는 메모리 풀은 비율이 올바르지 않을 수 있다. |
| `Process Open Files` | 프로세스가 열은 file descriptor 수 | 계속 증가하면 파일·소켓 누수를 의심한다. OS에 따라 지표가 없을 수 있다. |
| `CPU Usage` | 시스템과 JVM 프로세스 CPU 사용률 | 전체 시스템 포화인지 애플리케이션 집중 사용인지 구분한다. |
| `Load Average` | 1분 load average와 CPU 수 | load average를 CPU 수와 비교해 실행 대기 압력을 판단한다. |

## JVM Statistics - Memory / GC

| 패널 | 설명 |
| --- | --- |
| 선택한 heap/non-heap 풀 | 선택한 메모리 풀의 used·committed·max를 비교한다. GC 후에도 used 저점이 올라가는지 확인한다. |
| `Threads` | daemon·live·peak thread 수를 보여준다. live thread가 계속 늘어나는지 확인한다. |
| `Direct Buffers`, `Mapped Buffers` | JVM 밖의 direct·mapped buffer 사용량과 capacity를 보여준다. heap은 정상인데 RSS가 늘 때 확인한다. |
| `Memory Allocate/Promote` | 구간별 할당·promotion 속도를 보여준다. 요청량 대비 할당 증가와 Old Generation으로 옮겨지는 양을 확인한다. |
| `GC Count` | GC action·cause별 초당 발생 횟수를 보여준다. |
| `GC Stop the World Duration` | 단위 시간당 GC pause 누적 시간을 보여준다. 개별 pause의 최대 지연시간은 아니다. |

## Database Connection Pool HikariCP Statistics

| 패널 | 설명 |
| --- | --- |
| `Connections Size` | 선택한 pool의 전체 연결 수를 보여준다. |
| `Connections` | active·idle·pending 연결 수를 비교한다. active가 전체 크기에 붙고 pending이 발생하면 pool 포화를 의심한다. |
| `Connection Timeout Count` | 연결 획득 timeout의 프로세스 누적 횟수를 보여준다. 이미 높은 값보다 현재 증가하는지가 중요하다. |
| `Connection Creation Time` | 프로세스 기동 후 연결 생성 시간의 누적 평균을 보여준다. DB 네트워크·인증 지연을 확인한다. |
| `Connection Usage Time` | 연결이 반환되기까지 사용된 시간의 누적 평균을 보여준다. 긴 트랜잭션·쿼리를 의심한다. |
| `Connection Acquire Time` | pool에서 연결을 얻는 데 걸린 시간의 누적 평균을 보여준다. pending·timeout과 같이 증가하면 pool 포화 가능성이 크다. |

Creation·Usage·Acquire Time은 구간 rate가 아니라 누적 sum/count로 계산한 프로세스 수명 평균이다. 짧은 문제가 평균에 희석될 수 있으므로 `Connections`과 timeout 증가를 함께 본다.

## HTTP Statistics

| 패널 | 설명 |
| --- | --- |
| `Request Count` | actuator·health 경로를 제외한 HTTP 요청의 구간별 초당 횟수를 보여준다. URI·method·status 변화를 확인한다. |
| `Response Time` | exception이 없는 HTTP 요청의 구간 평균 응답 시간을 보여준다. 예외가 기록된 요청은 제외되므로 오류 지연을 대표하지 않는다. |

`Response Time`이 늘 때 HikariCP Acquire·Usage Time, CPU, GC 시간을 같은 시간대에 비교해 DB·CPU·GC 중 어디에서 지연이 시작됐는지 좁힌다.

## Logback Statistics

`INFO logs`, `ERROR logs`, `WARN logs`, `DEBUG logs`, `TRACE logs`는 레벨별 구간 로그 발생률을 보여준다. `INFO logs`에는 전체 `logback_events_total` 누적값도 함께 정의되어 있다. 로그 발생률은 이상 시점을 찾는 보조 지표이며 실제 원인은 애플리케이션 로그 본문에서 확인한다.

## 장애 확인 순서

1. `application`, `instance`, `hikaricp`, 시간 범위가 올바른지 확인한다.
2. `Uptime`과 `Start time`으로 재시작 여부를 확인한다.
3. `Request Count`, `Response Time`, error·warn 로그로 영향 시점을 확인한다.
4. HikariCP active·pending·timeout·acquire time으로 DB pool 병목을 먼저 배제한다.
5. CPU·load, thread, heap·GC 지표로 애플리케이션 자원 문제를 확인한다.

## 데이터가 없을 때

- Prometheus의 `arcade-backend` 타깃이 UP인지 확인한다.
- `/actuator/prometheus`에 `jvm_info`, `http_server_requests_seconds_count`, `hikaricp_connections`가 노출되는지 확인한다.
- Prometheus series에 `application="arcade-backend"` 라벨이 붙었는지 확인한다.
- Grafana의 `application`, `instance`, `hikaricp` 선택을 초기화한다.
- HikariCP 메트릭이 없으면 datasource·Actuator metrics 구성과 실제 pool 생성 여부를 확인한다.
