import type { ReactNode } from 'react';

interface BlockProps {
  title?: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  className?: string;
  reveal?: boolean;
}

/** 상세/마이페이지에서 반복되는 카드형 섹션 */
export function Block({ title, description, children, className, reveal }: BlockProps) {
  return (
    <section className={['block', className].filter(Boolean).join(' ')} data-reveal={reveal ? '' : undefined}>
      {title ? <h3 className="block-title">{title}</h3> : null}
      {description ? (
        <p style={{ fontSize: '.86rem', color: 'var(--text-dim)', marginBottom: '0.875rem' }}>{description}</p>
      ) : null}
      {children}
    </section>
  );
}

interface SideCardProps {
  title?: string;
  children: ReactNode;
  className?: string;
}

export function SideCard({ title, children, className }: SideCardProps) {
  return (
    <div className={['side-card', className].filter(Boolean).join(' ')}>
      {title ? <h4>{title}</h4> : null}
      {children}
    </div>
  );
}

/**
 * 상세 페이지 공통 2단 레이아웃.
 * 사이드바가 없으면 빈 칸을 남기지 않고 본문이 전체 폭을 쓴다.
 */
export function DetailGrid({ main, side }: { main: ReactNode; side?: ReactNode }) {
  return (
    <div className={side ? 'mypage-grid' : 'mypage-grid is-single'}>
      <div className="mypage-main">{main}</div>
      {side ? <aside className="mypage-side">{side}</aside> : null}
    </div>
  );
}
