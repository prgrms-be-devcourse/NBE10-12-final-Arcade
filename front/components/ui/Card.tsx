import type { HTMLAttributes, ReactNode } from 'react';

interface CardProps extends HTMLAttributes<HTMLElement> {
  children: ReactNode;
  surface?: 'default' | 'muted';
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hoverable?: boolean;
  as?: 'article' | 'section' | 'div';
}

/** 반복 surface에 쓰는 기본 카드. 도메인별 카드의 점진적 이관 대상이다. */
export function Card({
  children,
  surface = 'default',
  padding = 'md',
  hoverable = false,
  as: Component = 'article',
  className,
  ...rest
}: CardProps) {
  return <Component className={['ui-card', `ui-card--${surface}`, `ui-card--pad-${padding}`, hoverable ? 'ui-card--hoverable' : null, className].filter(Boolean).join(' ')} {...rest}>{children}</Component>;
}

export function Panel({ children, className, ...rest }: Omit<CardProps, 'as'>) {
  return <Card as="section" className={['ui-panel', className].filter(Boolean).join(' ')} {...rest}>{children}</Card>;
}
