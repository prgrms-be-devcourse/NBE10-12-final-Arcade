'use client';

import { useEffect } from 'react';

/**
 * `[data-reveal]` 요소가 뷰포트에 들어오면 `.in-view` 를 붙여 등장 애니메이션을 실행한다.
 * (목업의 IntersectionObserver 로직을 훅으로 옮긴 것)
 *
 * 라우트 이동뿐 아니라 탭 전환·조건부 렌더로도 새 `[data-reveal]` 이 생기므로,
 * MutationObserver 로 DOM 변화를 감지해 새로 나타난 요소까지 계속 관찰한다.
 * 다시 관찰하지 않으면 새로 그려진 섹션이 opacity:0 인 채로 남는다.
 */
export function useReveal() {
  useEffect(() => {
    // React hydration이 완료된 다음 DOM 클래스를 변경한다. 너무 이르게
    // classList를 바꾸면 서버 HTML과 클라이언트 HTML이 달라져 경고가 난다.
    let intersections: IntersectionObserver | null = null;
    let mutations: MutationObserver | null = null;
    let cancelled = false;

    const timer = window.setTimeout(() => {
      if (cancelled) return;

      // IntersectionObserver가 없는 환경에서는 애니메이션 없이 바로 보여준다.
      if (!('IntersectionObserver' in window)) {
        const showAll = () =>
          document
            .querySelectorAll('[data-reveal]:not(.in-view)')
            .forEach((el) => el.classList.add('in-view'));
        showAll();
        mutations = new MutationObserver(showAll);
        mutations.observe(document.body, { childList: true, subtree: true });
        return;
      }

      intersections = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('in-view');
              intersections?.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.12 },
      );

      const observeNew = () =>
        document
          .querySelectorAll('[data-reveal]:not(.in-view)')
          .forEach((el) => intersections?.observe(el));

      observeNew();
      mutations = new MutationObserver(observeNew);
      mutations.observe(document.body, { childList: true, subtree: true });
    }, 0);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      mutations?.disconnect();
      intersections?.disconnect();
    };
  }, []);
}
