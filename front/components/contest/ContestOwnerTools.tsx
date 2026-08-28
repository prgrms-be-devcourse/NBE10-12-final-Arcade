'use client';

import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { SideCard } from '@/components/ui/Block';
import { DeleteButton } from '@/components/ui/DeleteButton';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { deleteContest } from '@/lib/api';
import type { ContestDetail } from '@/lib/types';

/**
 * 대회 상세의 관리 도구.
 *
 * 볼 수 있는 사람은 두 종류다.
 * - 이 대회를 올린 주최측 계정 (소속 회사 id 가 같을 때)
 * - 관리자 — 주최측과 무관하게 모든 대회를 관리한다
 *
 * 로그인 정보를 불러오는 동안에는 아무것도 그리지 않는다.
 */
export function ContestOwnerTools({ contest }: { contest: ContestDetail }) {
  const me = useCurrentUser();
  if (!me) return null;

  const isAdmin = me.profile.memberRole === 'ADMIN';
  const isSameHost = Boolean(contest.hostId) && me.hostId === contest.hostId;
  if (!isAdmin && !isSameHost) return null;

  return (
    <SideCard title="대회 관리" className="leader-tools">
      <p className="leader-tools-note">
        {isAdmin && !isSameHost
          ? '관리자 권한으로 모든 대회를 관리할 수 있어요.'
          : `${contest.host} 소속이라 이 대회를 관리할 수 있어요.`}
      </p>
      <Link
        className="btn btn-ghost"
        style={{ width: '100%', marginBottom: '0.625rem' }}
        href={`/contests/create?edit=${contest.id}`}
      >
        <Icon name="i-pencil" />
        대회 정보 수정
      </Link>

      <div className="tool-danger-zone">
        <DeleteButton
          label="대회 삭제"
          confirmTitle="대회를 삭제할까요?"
          confirmDescription={`'${contest.title}' 등록 정보가 사라져요. 되돌릴 수 없습니다.`}
          onDelete={() => deleteContest(contest.id)}
          redirectTo="/contests"
          block
        />
      </div>
    </SideCard>
  );
}
