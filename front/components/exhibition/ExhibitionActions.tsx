'use client';

import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { DeleteButton } from '@/components/ui/DeleteButton';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { deleteExhibition } from '@/lib/api';
import type { UserSummary } from '@/lib/types';

interface ExhibitionActionsProps {
  exhibitionId: string;
  exhibitionTitle: string;
  /** 전시를 올린 사람 — 이 사람에게만 수정·삭제가 보인다 (파티와 같은 조건) */
  owner: UserSummary;
  githubUrl?: string;
}

/**
 * 전시 상세 액션 바 — 외부 링크는 누구에게나, 수정·삭제는 올린 사람에게만 보인다.
 * 좋아요·북마크는 대회 상세와 동일하게 헤더의 DetailActions 가 담당한다.
 */
export function ExhibitionActions({
  exhibitionId,
  exhibitionTitle,
  owner,
  githubUrl,
}: ExhibitionActionsProps) {
  const me = useCurrentUser();
  const isOwner = me?.profile.id === owner.id;

  return (
    <div className="exh-actions">
      {isOwner ? (
        <>
          <Link className="exh-action-btn" href={`/exhibition/create?edit=${exhibitionId}`}>
            <Icon name="i-pencil" />
            전시 수정
          </Link>
          <DeleteButton
            className="exh-action-btn"
            label="전시 삭제"
            confirmTitle="전시를 삭제할까요?"
            confirmDescription={`'${exhibitionTitle}' 전시와 달린 댓글이 모두 사라져요. 되돌릴 수 없습니다.`}
            onDelete={() => deleteExhibition(exhibitionId)}
            redirectTo="/exhibition"
          />
        </>
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
