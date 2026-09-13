'use client';

import { useRef } from 'react';
import type { ReactNode } from 'react';

export function PositionScroller({ children, className }: { children: ReactNode; className?: string }) {
  const listRef = useRef<HTMLDivElement>(null);

  const move = (direction: number) => {
    listRef.current?.scrollBy({ left: direction * 240, behavior: 'smooth' });
  };

  return <div className={['position-scroller', className].filter(Boolean).join(' ')}>
    <button type="button" className="position-scroll-btn" onClick={() => move(-1)} aria-label="모집 포지션 왼쪽으로 보기">‹</button>
    <div ref={listRef} className="position-list">{children}</div>
    <button type="button" className="position-scroll-btn" onClick={() => move(1)} aria-label="모집 포지션 오른쪽으로 보기">›</button>
  </div>;
}
