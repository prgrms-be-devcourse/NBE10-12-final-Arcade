import type { ReactNode } from 'react';

interface SectionHeadProps {
  title: string;
  description?: ReactNode;
  /** 우측에 붙는 버튼 등 */
  action?: ReactNode;
}

export function SectionHead({ title, description, action }: SectionHeadProps) {
  const head = (
    <div className="section-head">
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
    </div>
  );

  if (!action) return head;

  return (
    <div className="board-head-row">
      {head}
      {action}
    </div>
  );
}
