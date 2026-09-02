import type { ReactNode } from 'react';
import { GOAL_SOURCE_LABELS, POSITION_LABELS } from '@/lib/constants';
import type { GoalSource, PartyPosition } from '@/lib/types';

export function Tag({ children, accent }: { children: ReactNode; accent?: boolean }) {
  return <span className={accent ? 'tag accent-outline' : 'tag'}>{children}</span>;
}

export function TagRow({ children }: { children: ReactNode }) {
  return <div className="tag-row">{children}</div>;
}

export function SkillChip({ children }: { children: ReactNode }) {
  return <span className="skill-chip">{children}</span>;
}

export function ChipRow({ children }: { children: ReactNode }) {
  return <div className="chip-row">{children}</div>;
}

export function DDay({ children }: { children: ReactNode }) {
  return <span className="dday">{children}</span>;
}

/**
 * 성취 출처 배지.
 * '검증' 표현은 쓰지 않는다 — 자동기록은 활동 사실만 보장하지 품질·진위를 보증하지 않는다 (기획서 2.5).
 */
export function SourceBadge({ source }: { source: GoalSource }) {
  return (
    <span className={`goal-badges ${ source === 'PLATFORM_VERIFIED' ? 'src verified' : 'src self' }`}>
      {GOAL_SOURCE_LABELS[source]}
    </span>
  );
}

type PillTone = 'live' | 'review' | 'pending' | 'default';

export function StatusPill({ children, tone = 'default' }: { children: ReactNode; tone?: PillTone }) {
  const className = tone === 'default' ? 'status-pill' : `status-pill ${tone}`;
  return (
    <span className={className}>
      {tone === 'pending' ? <span className="dot" /> : null}
      {children}
    </span>
  );
}

/** 포지션 정원 칩 — 정원 0이면 미모집, 정원이 다 차면 마감 */
export function SlotChip({ position }: { position: PartyPosition }) {
  const { type, capacity, filledCount } = position;
  const state = capacity === 0 ? 'none' : filledCount >= capacity ? 'full' : '';
  return (
    <span className={['slot-chip', state].filter(Boolean).join(' ')}>
      {POSITION_LABELS[type]} {filledCount}/{capacity}
    </span>
  );
}
