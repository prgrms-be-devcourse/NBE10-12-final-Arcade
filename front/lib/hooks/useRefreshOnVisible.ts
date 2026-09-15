'use client';

import { useEffect, useRef } from 'react';

interface Options {
  /** 탭이 보이는 동안의 갱신 주기. 0 이면 주기 갱신 없이 포커스 복귀 때만 읽는다 */
  intervalMs?: number;
  /** 직전 갱신과 이 간격 안이면 건너뛴다. 포커스가 여러 번 튈 때 요청이 몰리는 걸 막는다 */
  minGapMs?: number;
}

/**
 * 창을 다시 볼 때 목록을 새로 읽는다 — 알림·쪽지 배지처럼 '새 것이 왔는지'가 중요한 곳에 쓴다.
 *
 * 서버에 푸시(SSE·웹소켓)가 없어서 결국 물어보는 수밖에 없는데, 그냥 주기 폴링을 돌리면
 * 아무도 안 보는 배경 탭에서까지 요청이 나간다. 그래서 세 가지로 낭비를 줄인다.
 *
 * - 탭이 숨겨져 있으면 아예 요청하지 않는다 (다시 보이는 순간 한 번 읽는다)
 * - 창 포커스가 돌아오면 읽는다 — 자리를 비웠다 온 경우가 사실상 전부다
 * - minGap 안에 다시 불리면 건너뛴다
 *
 * 푸시가 생기면 이 훅을 걷어내고 구독으로 바꾸면 된다
 * (docs/프론트-API연동_백엔드_수정요청.md ⑩).
 */
export function useRefreshOnVisible(
  refresh: () => void,
  { intervalMs = 60_000, minGapMs = 15_000 }: Options = {},
) {
  // 최신 콜백을 담아둔다 — 리스너를 매번 다시 걸지 않기 위해서다
  const refreshRef = useRef(refresh);
  const lastRunRef = useRef(0);

  useEffect(() => {
    refreshRef.current = refresh;
  }, [refresh]);

  useEffect(() => {
    // 마운트 직후의 첫 포커스로 방금 읽은 목록을 또 읽지 않게 시작점을 잡아둔다
    lastRunRef.current = Date.now();

    const run = () => {
      if (document.hidden) return;
      const now = Date.now();
      if (now - lastRunRef.current < minGapMs) return;
      lastRunRef.current = now;
      refreshRef.current();
    };

    const onVisibilityChange = () => {
      if (!document.hidden) run();
    };

    window.addEventListener('focus', run);
    document.addEventListener('visibilitychange', onVisibilityChange);
    // 배경 탭에서도 타이머는 돌지만 run() 이 곧바로 빠져나간다
    const timer = intervalMs > 0 ? window.setInterval(run, intervalMs) : undefined;

    return () => {
      window.removeEventListener('focus', run);
      document.removeEventListener('visibilitychange', onVisibilityChange);
      if (timer !== undefined) window.clearInterval(timer);
    };
  }, [intervalMs, minGapMs]);
}
