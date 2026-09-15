import type { ReactNode } from 'react';

interface SectionHeadProps {
  title: string;
  description?: ReactNode;
  /** 우측에 붙는 버튼 등 */
  action?: ReactNode;
}

export function SectionHead({ title, description, action }: SectionHeadProps) {
  const titleKind = title.includes('파티') ? 'party' : title.includes('공모전') || title.includes('대회') ? 'contest' : title.includes('전시') ? 'exhibition' : 'default';
  const head = (
    <div className={`section-head section-head--${titleKind}`}>
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
