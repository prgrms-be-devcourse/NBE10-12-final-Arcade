'use client';

import { useEffect, useState } from 'react';

/**
 * 아래로 스크롤하면 숨고, 위로 올리면 다시 나타나는 헤더 동작.
 *
 * 판단 기준은 '현재 위치'가 아니라 '직전 위치와의 방향'이다.
 * 그래야 페이지 중간에서 조금만 올려도 바로 메뉴를 꺼낼 수 있다.
 *
 * 예외로 두는 상황:
 * - 맨 위 근처(헤더 높이 이내)에서는 항상 보인다. 문서가 짧아 그만큼 내려갈 수 없으면
 *   이 조건에 늘 걸리므로, '되돌릴 스크롤이 없어 헤더를 영영 못 꺼내는' 상황도 같이 막힌다.
 * - 아주 작은 흔들림(트랙패드 관성, 모바일 주소창 접힘)은 방향으로 치지 않는다.
 *
 * 계산은 scrollY 한 번 읽는 게 전부라 requestAnimationFrame 으로 묶지 않았다.
 * rAF 로 묶으면 탭이 백그라운드로 내려간 사이 콜백이 밀려 '대기 중' 플래그가 남고,
 * 돌아왔을 때 스크롤에 반응하지 않는 상태가 될 수 있다.
 */
export function useHideOnScroll(): boolean {
  const [hidden, setHidden] = useState(false);

  useEffect(() => {
    const rem = parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
    /** 헤더 높이(4.5rem)만큼 내려가기 전에는 숨기지 않는다 */
    const revealZone = rem * 4.5;
    /** 이보다 작은 움직임은 방향으로 치지 않는다 */
    const noise = rem * 0.5;

    let lastY = window.scrollY;

    const onScroll = () => {
      const y = window.scrollY;
      const delta = y - lastY;

      if (Math.abs(delta) < noise) return;
      lastY = y;

      setHidden(y > revealZone && delta > 0);
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return hidden;
}
