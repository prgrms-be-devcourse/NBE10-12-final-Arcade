'use client';

import { useState, type PointerEvent, type ReactNode } from 'react';

export function InteractiveTitle({ children, className = 'block-title', as = 'h3' }: { children: ReactNode; className?: string; as?: 'h1' | 'h2' | 'h3' }) {
  const [pressed, setPressed] = useState(false);
  const Heading = as;
  const press = (event: PointerEvent<HTMLElement>) => {
    if (event.button === 0 || event.pointerType === 'touch') setPressed(true);
  };
  const release = () => setPressed(false);
  return (
    <div className={['interactive-title', pressed ? 'is-pressed' : null].filter(Boolean).join(' ')}>
      <button type="button" className="title-arcade-button" aria-label="제목 장식 버튼" aria-pressed={pressed} onPointerDown={press} onPointerUp={release} onPointerCancel={release} onPointerLeave={release}>
        <svg className="title-arcade-base" viewBox="0 0 32 44" aria-hidden="true" focusable="false"><use href="/images/arcade-button.svg#base" /></svg>
        <svg className="title-arcade-fill" viewBox="0 0 32 44" aria-hidden="true" focusable="false"><use href="/images/arcade-button.svg#body-fill" /></svg>
        <svg className="title-arcade-body" viewBox="0 0 32 44" aria-hidden="true" focusable="false"><use href="/images/arcade-button.svg#body" /></svg>
      </button>
      <Heading className={className}>{children}</Heading>
    </div>
  );
}
