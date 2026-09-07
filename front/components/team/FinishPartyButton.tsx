'use client';

import { useState } from 'react';
import Link from 'next/link';
import { finishParty } from '@/lib/api';

export function FinishPartyButton({ partyId }: { partyId: string }) {
  const [done, setDone] = useState(false);

  return (
    <div className="side-card leader-tools">
      <h4>파티장 도구</h4>
      <p className="leader-tools-note">
        작업을 마치면 파티를 완료 처리할 수 있어요. 완료하면 결과물과 커밋 기록이 참여자 전원의
        성취 프로필에 자동 저장됩니다.
      </p>
      <button
        type="button"
        className="btn btn-primary"
        disabled={done}
        onClick={async () => {
      await finishParty(partyId);
          setDone(true);
        }}
      >
        파티 진행 완료
      </button>
      {done ? (
        <p className="tool-done-note">
          파티가 완료 처리됐어요. 참여자 전원의 성취 프로필과 전시관에 기록이 등록됩니다.
        </p>
      ) : null}

      {/*
        전시는 파티 단위로 게시한다(POST /parties/{partyId}/showcase). 파티장만 게시할 수 있고,
        전시관 목록에는 게시된 파티만 뜬다. 실제 권한은 서버가 403 으로 가른다.
      */}
      <p className="leader-tools-note" style={{ marginTop: '0.875rem' }}>
        완료한 파티는 전시관에 공개할 수 있어요. 제목과 설명만 적으면 참여 팀원과 저장소는 파티
        정보에서 따라옵니다.
      </p>
      <Link className="btn btn-ghost" href={`/exhibition/create?partyId=${partyId}`}>
        전시 게시
      </Link>
    </div>
  );
}
