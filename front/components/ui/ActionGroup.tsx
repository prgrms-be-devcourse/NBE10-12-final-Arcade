import type { ReactNode } from 'react';

interface ActionGroupProps {
  children: ReactNode;
  className?: string;
}

/** 버튼 묶음의 공통 구조. 정렬·반응형은 modifier class로만 확장한다. */
export function ActionGroup({ children, className }: ActionGroupProps) {
  return <div className={['action-group', className].filter(Boolean).join(' ')}>{children}</div>;
}
