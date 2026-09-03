/**
 * API 클라이언트.
 *
 * `.env.local` 의 두 값으로 동작이 갈린다.
 *
 *   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
 *   NEXT_PUBLIC_USE_MOCK=false   # true 면 주소가 있어도 데모 데이터를 쓴다
 *
 * 서버 주소를 그대로 둔 채 데모로 되돌리고 싶으면 NEXT_PUBLIC_USE_MOCK=true 만 주면 된다.
 *
 * 백엔드는 모든 응답을 { resultCode, msg, data } 봉투로 감싼다(기획서 8장).
 * request() 가 그 껍데기를 벗겨 data 만 돌려주므로, 각 api 모듈은 data 타입만 신경 쓰면 된다.
 */

/**
 * 브라우저가 부르는 주소. NEXT_PUBLIC_ 이라 빌드 시점에 번들로 인라인된다.
 * WebSocket 주소와 OAuth 오리진 계산도 이 값을 쓴다(둘 다 브라우저 전용).
 */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

/**
 * 데모 데이터 사용 여부.
 *
 * 명시적으로 준 값이 항상 이긴다. 아무것도 주지 않았을 때만 서버 주소 유무로 판단한다.
 * (예전에는 주소가 있으면 USE_MOCK=true 를 줘도 무시돼서, 데모로 되돌리려면 주소까지 지워야 했다)
 */
export const USE_MOCK = (() => {
  const flag = process.env.NEXT_PUBLIC_USE_MOCK;
  if (flag === "true") return true;
  if (flag === "false") return false;
  return !API_BASE_URL;
})();
/**
 * 서버(SSR)가 부르는 주소. **NEXT_PUBLIC_ 접두사가 없다** — 번들에 박히지 않고
 * 런타임에 읽히므로 컨테이너 환경변수로 주입할 수 있다.
 *
 * 컨테이너 안에서 localhost 는 자기 자신이라 브라우저와 같은 값을 쓸 수 없다.
 * 비어 있으면 공개 주소로 떨어진다 (호스트에서 직접 실행하는 개발 환경).
 */
const INTERNAL_API_BASE_URL = process.env.API_BASE_URL_INTERNAL ?? "";

/** 호출 시점에 따라 베이스 주소가 갈린다. */
function apiBase(): string {
  if (typeof window === "undefined" && INTERNAL_API_BASE_URL)
    return INTERNAL_API_BASE_URL;
  return API_BASE_URL;
}

/** 목 응답에 약간의 지연을 줘서 로딩 상태를 실제처럼 확인할 수 있게 한다. */
const MOCK_LATENCY_MS = 180;

/** 백엔드 공통 응답 봉투 (기획서 8장) */
export interface ApiEnvelope<T> {
  /** "200-1", "400-4", "409-2" 처럼 HTTP 상태 + 도메인 번호 */
  resultCode: string;
  msg: string;
  data: T;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body?: unknown,
    /** 백엔드 resultCode. 화면에서 409-2 같은 구체 상황을 구분할 때 쓴다 */
    readonly resultCode?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** 봉투인지 판별한다. 봉투가 아니면(예: 프록시 오류) 그대로 쓴다. */
function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return (
    typeof value === "object" &&
    value !== null &&
    "resultCode" in value &&
    "data" in value
  );
}

export function delay(ms = MOCK_LATENCY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 목 데이터를 API 호출처럼 감싸 반환한다. 참조 공유를 막기 위해 깊은 복사한다. */
export async function mockResponse<T>(data: T, ms?: number): Promise<T> {
  await delay(ms);
  return structuredClone(data);
}

export interface RequestOptions extends Omit<RequestInit, "body"> {
  /** 쿼리 스트링으로 붙일 값들 (undefined 는 제외됨) */
  query?: Record<string, string | number | boolean | undefined | null>;
  body?: unknown;
}

/**
 * 보호 API가 401을 반환한 뒤에는 만료·변조된 access/refresh 쿠키를 서버에서 지운다.
 *
 * 쿠키가 HttpOnly라 프론트에서 직접 삭제할 수 없으며, 동시에 여러 화면 컴포넌트가 인증 API를
 * 호출할 수 있어 진행 중인 정리 요청은 하나만 공유한다. 로그인 실패처럼 토큰 유효성과 무관한
 * 공개 인증 엔드포인트의 401은 이 처리에서 제외한다.
 */
let invalidSessionCleanup: Promise<void> | null = null;
const SESSION_ENDPOINT_PATHS = new Set([
  "/members/login",
  "/members/logout",
  "/members/signup",
  "/members/refresh",
]);

async function clearInvalidSessionCookies(path: string): Promise<void> {
  if (
    USE_MOCK ||
    typeof window === "undefined" ||
    SESSION_ENDPOINT_PATHS.has(path)
  ) {
    return;
  }

  if (!invalidSessionCleanup) {
    invalidSessionCleanup = fetch(buildUrl("/members/logout"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
    })
      // 로그아웃 호출 자체가 실패해도 원래 401 오류를 가리거나 미처리 Promise를 만들면 안 된다.
      .catch(() => undefined)
      .then(() => undefined)
      .finally(() => {
        invalidSessionCleanup = null;
      });
  }

  await invalidSessionCleanup;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.append(key, String(value));
    }
  });
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

/**
 * 서버 컴포넌트에서 호출할 때 브라우저 쿠키를 대신 실어 준다.
 *
 * 인증 토큰이 쿠키에 들어 있는데, 서버 컴포넌트의 fetch 는 Node 에서 실행돼
 * credentials:'include' 가 아무 일도 하지 않는다. 그래서 요청에 담겨 온 쿠키를 직접 옮긴다.
 * 브라우저에서는 이 함수가 곧바로 undefined 를 돌려주고 평소대로 credentials 로 처리된다.
 */
async function serverCookieHeader(): Promise<string | undefined> {
  if (typeof window !== "undefined") return undefined;
  try {
    const { cookies } = await import("next/headers");
    const store = await cookies();
    return store.toString() || undefined;
  } catch (error) {
    // 정적 생성 bailout 은 Next 의 제어 흐름이다. 삼키면 Next 가 이 라우트를
    // 동적으로 승격시키지 못해 빌드 때 백엔드를 호출하게 된다. 그것만 다시 던진다.
    const { unstable_rethrow } = await import("next/navigation");
    unstable_rethrow(error);
    // 그 밖의 경우(요청 컨텍스트 밖 호출 등)는 쿠키 없이 진행한다
    return undefined;
  }
}

/** 실제 서버 호출용 공통 fetch 래퍼 */
export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { query, body, headers, ...rest } = options;

  const cookie = await serverCookieHeader();

  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(cookie ? { Cookie: cookie } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: "include",
  });

  if (!response.ok) {
    let errorBody: unknown;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = await response.text();
    }

    // 예외도 같은 봉투로 내려온다. msg 를 그대로 화면 문구로 쓸 수 있다.
    const message = isEnvelope<unknown>(errorBody)
      ? errorBody.msg
      : `API 요청 실패: ${response.status}`;
    const resultCode = isEnvelope<unknown>(errorBody)
      ? errorBody.resultCode
      : undefined;

    // 토큰이 잘못되어 보호 API 접근이 거부된 경우, HttpOnly access/refresh 쿠키를 서버에서
    // 함께 지운다. 다음 화면에서는 비로그인 상태로 정상 진입한다.
    if (response.status === 401) {
      await clearInvalidSessionCookies(path);
    }

    throw new ApiError(message, response.status, errorBody, resultCode);
  }

  // 삭제 응답은 본문이 없을 수 있다
  if (response.status === 204) return undefined as T;

  const payload: unknown = await response.json();
  return (isEnvelope<T>(payload) ? payload.data : payload) as T;
}

export const http = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PATCH", body }),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "DELETE" }),
};
