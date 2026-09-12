'use client';

import type { ReactNode } from 'react';

export function PositionScroller({ children, className }: { children: ReactNode; className?: string }) {
  const move = (direction: number) => {
    document.querySelector<HTMLElement>('.position-list')?.scrollBy({ left: direction * 240, behavior: 'smooth' });
  };

  return <div className={['position-scroller', className].filter(Boolean).join(' ')}>
    <button type="button" className="position-scroll-btn" onClick={() => move(-1)} aria-label="모집 포지션 왼쪽으로 보기">‹</button>
    <div className="position-list">{children}</div>
    <button type="button" className="position-scroll-btn" onClick={() => move(1)} aria-label="모집 포지션 오른쪽으로 보기">›</button>
  </div>;
}
