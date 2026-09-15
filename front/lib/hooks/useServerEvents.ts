'use client';

import { useEffect, useRef } from 'react';
import { API_BASE_URL, USE_MOCK } from '@/lib/api/client';

/**
 * 서버가 밀어주는 이벤트(SSE)를 구독한다.
 *
 * 인증이 쿠키라 `withCredentials` 가 필요하고, 백엔드가 다른 오리진이므로 서버쪽 CORS 가
 * `allowCredentials` + 이 오리진 허용이어야 한다(SecurityConfig 의 corsConfigurationSource).
 *
 * EventSource 는 끊기면 브라우저가 알아서 다시 붙는다. 다만 끊긴 동안 온 이벤트는 사라지므로,
 * 재연결 때마다 오는 `connect` 이벤트에서 목록을 한 번 다시 읽어 빈 구간을 메꾸는 쪽을 권한다.
 *
 * @param path   API_BASE_URL 뒤에 붙일 경로. null 이면 구독하지 않는다(파티 id 가 없을 때 등).
 * @param handlers 이벤트 이름 → 핸들러. 키 목록은 첫 구독 때 정해지므로 고정된 형태로 넘긴다.
 */
export function useServerEvents(
  path: string | null,
  handlers: Record<string, (data: string) => void>,
) {
  // 핸들러는 매 렌더 새 객체다. 의존성에 넣으면 렌더마다 연결을 다시 맺으므로 ref 로 최신 것만 본다.
  const latest = useRef(handlers);

  // 렌더 중에 ref 를 쓰면 안 되므로 갱신은 effect 에서 한다.
  // 이 effect 가 아래 구독보다 먼저 선언돼 있어 재렌더 시 항상 최신 핸들러가 먼저 꽂힌다.
  useEffect(() => {
    latest.current = handlers;
  });

  useEffect(() => {
    if (!path || USE_MOCK || !API_BASE_URL) return;

    const source = new EventSource(`${API_BASE_URL}${path}`, { withCredentials: true });
    const bound = Object.keys(latest.current).map((name) => {
      const listener = (event: MessageEvent) => latest.current[name]?.(event.data);
      source.addEventListener(name, listener);
      return { name, listener };
    });

    return () => {
      bound.forEach(({ name, listener }) => source.removeEventListener(name, listener));
      source.close();
    };
  }, [path]);
}
