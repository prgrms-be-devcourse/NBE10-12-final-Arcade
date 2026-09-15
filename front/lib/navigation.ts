'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';

/**
 * 수정 화면을 떠날 때 쓰는 이동.
 *
 * 취소·저장으로 `router.push(상세)` 를 하면 히스토리에 항목이 하나 더 쌓인다 —
 * `목록 → 상세 → 수정 → 상세` 가 되어, 상세에서 뒤로가기를 누르면 목록이 아니라
 * **수정 화면이 다시 나온다.** 되돌아가는 동작이므로 히스토리를 앞으로 밀면 안 된다.
 *
 * 다만 새 탭에서 수정 URL 을 바로 연 경우에는 돌아갈 항목이 없어 back 이 아무 일도 하지 않는다.
 * 그때만 fallback 경로로 보낸다.
 */
export function useLeaveTo(fallbackHref: string): () => void {
  const router = useRouter();

  return useCallback(() => {
    if (typeof window !== 'undefined' && window.history.length > 1) {
      router.back();
      return;
    }
    router.replace(fallbackHref);
  }, [router, fallbackHref]);
}
