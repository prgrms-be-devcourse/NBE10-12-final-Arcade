# JVM (Micrometer) 대시보드

`jvm-micrometer.json`은 Arcade 백엔드의 JVM·프로세스·HTTP 지표를 한 화면에서 보는 심층 분석용 대시보드다. Micrometer가 노출하는 지표를 사용하며 Grafana UID는 `arcade-jvm-micrometer`다.

## 변수

| 변수 | 의미 |
| --- | --- |
| `application` | Prometheus의 `application` 라벨로 애플리케이션을 선택한다. 현재 수집 설정은 `arcade-backend`를 붙인다. |
| `instance` | 선택한 애플리케이션의 Prometheus 타깃을 선택한다. |
| `jvm_memory_pool_heap` | Eden, Survivor, Old Generation 등 힙 메모리 풀을 선택한다. 이름은 GC 종류에 따라 달라진다. |
| `jvm_memory_pool_nonheap` | Metaspace, Code Cache 등 비힙 메모리 풀을 선택한다. |
| `jvm_buffer_pool` | Direct, mapped 등 JVM 버퍼 풀을 선택한다. |

## 빠른 상태 확인

| 패널 | 보여주는 값 |
| --- | --- |
| `Uptime`, `Start time` | 프로세스 기동 시각과 실행 시간 |
| `Heap used`, `Non-Heap used` | used/max 메모리 비율 |
| `Rate` | 1분 기준 HTTP 초당 요청 수 |
| `Errors` | 1분 기준 HTTP 5xx 초당 발생 수 |
| `Duration` | 5xx를 제외한 평균 및 최대 응답 시간 |

## JVM·프로세스

| 패널 | 설명 |
| --- | --- |
| `JVM Heap`, `JVM Non-Heap`, `JVM Total` | used·committed·max 메모리를 비교한다. GC 후에도 used의 저점이 계속 올라가면 장기 생존 객체 증가를 확인한다. |
| `CPU Usage` | 시스템 CPU, JVM 프로세스 CPU, 프로세스 CPU의 15분 평균을 비교한다. |
| `Load` | 1분 load average와 CPU 수를 보여준다. load average는 CPU 수와 같이 해석한다. |
| `Threads`, `Thread States` | live·daemon·peak thread와 JVM thread 상태를 보여준다. thread 수가 계속 늘거나 blocked 상태가 증가하는지 확인한다. |
| `Log Events` | 최근 1분의 Logback 레벨별 로그 증가량을 보여준다. error·warn 증가를 HTTP 오류와 비교한다. |
| `File Descriptors` | 열린 파일 descriptor와 최대치를 비교한다. 지원하지 않는 OS에서는 데이터가 없을 수 있다. |

## 메모리 풀·GC·런타임

| 패널 | 설명 |
| --- | --- |
| 선택한 heap/non-heap 풀 | 풀별 used·committed·max를 보여준다. 전체 메모리 이상을 특정 풀로 좁힐 때 사용한다. |
| `GC Pressure` | 최근 1분의 GC pause 시간을 CPU 수로 나눈 비율이다. 지속적으로 올라가면 할당량과 heap 압력을 함께 본다. |
| `Collections` | GC action·cause별 초당 collection 횟수를 보여준다. |
| `Pause Durations` | GC pause의 평균과 최대 시간을 보여준다. 최대 pause 증가는 HTTP tail latency와 같이 확인한다. |
| `Allocated/Promoted` | 초당 메모리 할당·promotion 속도를 보여준다. 할당은 요청량과, promotion은 Old Generation 사용량과 함께 해석한다. |
| `Classes loaded`, `Class delta` | 현재 로드된 class 수와 1분 변화량을 보여준다. 동적 class 생성이 계속되는지 확인할 때 사용한다. |
| 선택한 buffer pool | direct·mapped 버퍼의 used·capacity·count를 보여준다. heap은 정상인데 프로세스 메모리가 늘 때 확인한다. |

## 장애 확인 순서

1. `application`, `instance`, 시간 범위가 올바른지 확인한다.
2. `Uptime`과 `Start time`으로 재시작 여부를 확인한다.
3. `Rate`, `Errors`, `Duration`으로 문제 시점과 요청량을 맞춘다.
4. `CPU Usage`와 thread 지표로 CPU 또는 thread 병목을 확인한다.
5. heap 저점, `GC Pressure`, pause, allocation/promotion으로 메모리·GC 영향을 확인한다.
6. JVM 지표가 정상이면 HikariCP를 보여주는 Spring Boot Statistics 대시보드에서 DB 연결 병목을 확인한다.

## 데이터가 없을 때

- Prometheus의 `arcade-backend` 타깃이 UP인지 확인한다.
- `/actuator/prometheus`에 `jvm_memory_used_bytes`와 `process_uptime_seconds`가 노출되는지 확인한다.
- Prometheus series에 `application="arcade-backend"` 라벨이 붙었는지 확인한다.
- 대시보드의 `application`과 `instance` 선택을 초기화한다.
- 해당 JVM·OS·서블릿 컨테이너가 지표를 제공하지 않는 패널은 빈 화면이 정상일 수 있다.
