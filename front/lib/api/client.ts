/**
 * API 클라이언트.
 *
 * 아직 백엔드 API가 확정되지 않아 기본값은 데모(mock) 모드다.
 * `.env.local` 에 아래 두 값을 넣으면 그대로 실제 서버를 호출한다.
 *
 *   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
 *   NEXT_PUBLIC_USE_MOCK=false
 */

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';

/** 데모 데이터 사용 여부. API가 붙으면 false 로 바꾸면 된다. */
export const USE_MOCK =
  process.env.NEXT_PUBLIC_USE_MOCK !== 'false' && !API_BASE_URL;

/** 목 응답에 약간의 지연을 줘서 로딩 상태를 실제처럼 확인할 수 있게 한다. */
const MOCK_LATENCY_MS = 180;

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function delay(ms = MOCK_LATENCY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 목 데이터를 API 호출처럼 감싸 반환한다. 참조 공유를 막기 위해 깊은 복사한다. */
export async function mockResponse<T>(data: T, ms?: number): Promise<T> {
  await delay(ms);
  return structuredClone(data);
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  /** 쿼리 스트링으로 붙일 값들 (undefined 는 제외됨) */
  query?: Record<string, string | number | boolean | undefined | null>;
  body?: unknown;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, String(value));
    }
  });
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

/** 실제 서버 호출용 공통 fetch 래퍼 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { query, body, headers, ...rest } = options;

  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: 'include',
  });

  if (!response.ok) {
    let errorBody: unknown;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = await response.text();
    }
    throw new ApiError(`API 요청 실패: ${response.status}`, response.status, errorBody);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const http = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'DELETE' }),
};
