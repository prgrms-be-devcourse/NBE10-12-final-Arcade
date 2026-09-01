'use client';

import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { SideCard } from '@/components/ui/Block';
import { DeleteButton } from '@/components/ui/DeleteButton';
import { deleteGoal, type GoalDetailResponse } from '@/lib/api/goals';

/**
 * 성취 상세의 관리 도구 — 본인이 직접 등록한 성취에만 보인다.
 *
 * 파티 활동으로 자동기록된 성취(PLATFORM_VERIFIED)는 서버가 수정·삭제를 막으므로(409-1)
 * 아예 그리지 않는다. 안내 문구는 본문 위쪽에 이미 떠 있다.
 */
export function GoalOwnerTools({ goal }: { goal: GoalDetailResponse }) {
  if (goal.source !== 'SELF_REPORTED') return null;

  const title = goal.detail.title ?? goal.detail.contestName ?? '이 성취';

  return (
    <SideCard title="성취 관리" className="leader-tools">
      <p className="leader-tools-note">직접 등록한 성취라 언제든 고치거나 지울 수 있어요.</p>

      <Link
        className="btn btn-ghost"
        style={{ width: '100%', marginBottom: '0.625rem' }}
        href={`/goals/${goal.id}/edit`}
      >
        <Icon name="i-pencil" />
        성취 수정
      </Link>

      <div className="tool-danger-zone">
        <DeleteButton
          label="성취 삭제"
          confirmTitle="성취를 삭제할까요?"
          confirmDescription={`'${title}' 기록이 사라져요. 되돌릴 수 없습니다.`}
          onDelete={() => deleteGoal(goal.id)}
          redirectTo="/mypage"
          block
        />
      </div>
    </SideCard>
  );
}
