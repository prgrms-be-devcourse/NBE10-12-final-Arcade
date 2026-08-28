'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { DeleteButton } from '@/components/ui/DeleteButton';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { closePartyRecruit, deleteParty } from '@/lib/api';
import type { PartyDetail } from '@/lib/types';

/**
 * 파티 상세 사이드바의 파티장 도구.
 *
 * 파티를 만든 사람에게만 보인다. 로그인 정보를 불러오는 동안에는 아무것도 그리지 않아,
 * 남의 파티에서 도구가 잠깐 보였다 사라지는 일이 없게 했다.
 */
export function LeaderTools({ party }: { party: PartyDetail }) {
  const router = useRouter();
  const me = useCurrentUser();
  const [closed, setClosed] = useState(false);

  const filled = party.positions.reduce((sum, position) => sum + position.filledCount, 0);
  const total = party.positions.reduce((sum, position) => sum + position.capacity, 0);

  const isLeader = me?.profile.id === party.leader.id;
  if (!isLeader) return null;

  const close = async () => {
    await closePartyRecruit(party.id);
    setClosed(true);
  };

  return (
    <div className="side-card leader-tools">
      <h4>파티장 도구</h4>
      <p className="leader-tools-note">
        현재 {filled}/{total}명 합류했어요. 더 이상 지원을 받지 않으려면 모집을 마감하세요. 마감하면
        파티 상태가 &apos;모집 완료&apos;로 바뀝니다.
      </p>
      <button
        type="button"
        className="btn btn-ghost"
        style={{ marginBottom: '0.625rem' }}
        onClick={() => router.push(`/party/create?edit=${party.id}`)}
      >
        파티 정보 수정
      </button>
      <button type="button" className="btn btn-ghost" onClick={close} disabled={closed}>
        인원 모집 완료
      </button>
      {closed ? (
        <p className="tool-done-note">모집이 마감됐어요. 지원 버튼이 닫히고 팀 스페이스가 열립니다.</p>
      ) : null}

      <div className="tool-danger-zone">
        <DeleteButton
          label="파티 삭제"
          confirmTitle="파티를 삭제할까요?"
          confirmDescription={`'${party.title}' 모집글과 지원 내역이 모두 사라져요. 되돌릴 수 없습니다.`}
          onDelete={() => deleteParty(party.id)}
          redirectTo="/party"
          block
        />
      </div>
    </div>
  );
}
