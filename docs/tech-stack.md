# 기술 스택과 프로젝트 관례

LLM 리뷰 봇의 `# Project context`로 프롬프트에 실린다. 사실만 적고, 확인되지 않은 것은 쓰지 않는다.

## 백엔드

| 항목              | 버전                             | 근거                                         |
| ----------------- | -------------------------------- | -------------------------------------------- |
| Java              | 25 (toolchain)                   | `back/build.gradle.kts`                      |
| Gradle            | 9.5.1 (wrapper)                  | `gradle-wrapper.properties`                  |
| Spring Boot       | 4.1.0                            | `build.gradle.kts`                           |
| springdoc-openapi | 3.1.0 (webmvc-ui)                | 명시 선언                                    |
| JJWT              | 0.13.0                           | 명시 선언                                    |
| DB                | H2(runtime), PostgreSQL(runtime) | 명시 선언                                    |
| Redis             | spring-data-redis 4.1.0 / lettuce-core 7.5.2 | Spring Boot BOM |
| Testcontainers    | 2.0.5, 이미지 `redis:7.0.8-alpine` | `RedisTestContainerConfig` |
| JaCoCo            | 0.8.15                           | `jacoco` 플러그인, Java 25 지원은 0.8.14부터 |

### 응답과 예외

공통 응답 래퍼 `RsData<T>`를 쓴다.

```java
public record RsData<T>(String resultCode, @JsonIgnore int statusCode, String msg, T data)
```

`resultCode`는 `"200-1"`, `"400-4"`, `"409-20"` 형태의 문자열이고, 앞자리가 HTTP 상태 코드가 된다. 예외는 `ServiceException(resultCode, msg)`을 던지고 `GlobalExceptionHandler`가 `RsData`로 변환한다.

## 프론트엔드

| 항목          | 버전                                            |
| ------------- | ----------------------------------------------- |
| Next.js       | 16.3.2 (App Router)                             |
| React         | 19.2.8                                          |
| TypeScript    | 5.9.3                                           |
| ESLint        | 9.39.5 + eslint-config-next 16.3.2              |
| Tailwind CSS  | 4.3.3 (`@tailwindcss/postcss`)                  |
| 패키지 매니저 | pnpm 11 (`pnpm-lock.yaml`, lockfileVersion 9.0) |
| Node.js       | 20.9 이상 (Next 16 요구)                        |

### Next.js 16에서 달라진 것

- **Turbopack이 기본**이다. `next dev`/`next build`에 `--turbopack` 플래그가 필요 없다
- **`next lint`가 제거**됐다. ESLint를 직접 실행하고, `next build`는 린트를 돌리지 않는다
- ESLint는 **flat config**(`eslint.config.mjs`)를 쓴다
- `params`, `searchParams`, `cookies()`, `headers()`는 **모두 async**다. 동기 접근은 제거됐다
- `middleware`가 `proxy`로 이름이 바뀌었다
