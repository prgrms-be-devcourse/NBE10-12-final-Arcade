import type { ReactNode } from 'react';

export type NoticeTone = 'info' | 'success' | 'warning' | 'error';

interface NoticeProps {
  tone?: NoticeTone;
  children: ReactNode;
  className?: string;
  role?: 'alert' | 'status';
}

export function Notice({ tone = 'info', children, className, role }: NoticeProps) {
  const liveRole = role ?? (tone === 'error' || tone === 'warning' ? 'alert' : 'status');
  return <div className={['notice', `notice--${tone}`, className].filter(Boolean).join(' ')} role={liveRole}>{children}</div>;
}

export function FieldMessage({ tone = 'info', children }: Omit<NoticeProps, 'className' | 'role'>) {
  return <p className={['field-message', `field-message--${tone}`].join(' ')} role={tone === 'error' ? 'alert' : 'status'}>{children}</p>;
}
