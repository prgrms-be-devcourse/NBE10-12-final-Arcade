'use client';

import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import type { UserSummary } from '@/lib/types';

interface ExhibitionActionsProps {
  partyId: string;
  /**
   * 전시를 올린 사람 — 이 사람에게만 수정이 보인다.
   *
   * 서버 전시 상세는 소유자를 이름으로만 준다(PartyShowcaseDto.ownerName). id 가 비어 있으면
   * 본인 여부를 가릴 수 없어 수정 버튼이 숨는다 - 파티장은 팀 스페이스의 '전시 게시'로 들어온다.
   * 응답에 ownerId 가 생기면 여기서 바로 켜진다 (요청서 ⑥).
   */
  owner: UserSummary;
  githubUrl?: string;
}

/**
 * 전시 상세 액션 바 — 외부 링크는 누구에게나, 수정은 올린 사람에게만 보인다.
 * 좋아요·북마크는 대회 상세와 동일하게 헤더의 DetailActions 가 담당한다.
 */
export function ExhibitionActions({ partyId, owner, githubUrl }: ExhibitionActionsProps) {
  const me = useCurrentUser();
  const isOwner = Boolean(me && owner.name && me.profile.name === owner.name);

  return (
    <div className="exh-actions">
      {/*
        전시 삭제(게시 취소)는 서버에 API 가 없어 뺐다 - POST /parties/{partyId}/showcase 만 있다.
        수정은 같은 POST 로 제목·설명을 다시 보내는 것이라 게시 화면을 그대로 연다.
      */}
      {isOwner ? (
        <Link className="exh-action-btn" href={`/exhibition/create?partyId=${partyId}`}>
          <Icon name="i-pencil" />
          전시 수정
        </Link>
      ) : null}

      {githubUrl ? (
        <a className="exh-action-btn" href={githubUrl} target="_blank" rel="noopener noreferrer">
          <Icon name="i-external" />
          GitHub 보기
        </a>
      ) : null}
    </div>
  );
}
