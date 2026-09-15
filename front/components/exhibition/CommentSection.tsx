'use client';

import { useCallback, useState } from 'react';
import { CommentThread, countComments } from '@/components/comment/CommentThread';
import {
  createExhibitionComment,
  deleteExhibitionComment,
  fetchExhibitionComments,
  updateExhibitionComment,
} from '@/lib/api';
import type { ThreadComment, UserProfile } from '@/lib/types';

interface CommentSectionProps {
  /** 전시는 파티에 종속이라 이 값은 partyId 다 */
  exhibitionId: string;
  comments: ThreadComment[];
  /**
   * 보고 있는 사람. 비로그인이면 null 이다.
   *
   * 클라이언트에서 다시 묻지 않고 페이지가 서버에서 읽은 값을 받는다 -
   * 훅으로 읽으면 첫 렌더에 '로그인해 주세요' 가 스쳤다가 입력칸으로 바뀐다.
   */
  viewer: UserProfile | null;
}

/**
 * 전시 상세 댓글 (ARC-160) — 1단계 답글, 작성자·관리자만 수정·삭제.
 *
 * 서버가 삭제를 soft delete 로 처리해서, 지운 댓글은 사라지지 않고 '삭제된 댓글' 자리로 남는다.
 * 그래야 그 아래 답글이 살아남는다. 그래서 변경 뒤에는 목록을 다시 읽는다.
 */
export function CommentSection({
  exhibitionId,
  comments: initialComments,
  viewer,
}: CommentSectionProps) {
  const [comments, setComments] = useState(initialComments);

  const refresh = useCallback(async () => {
    setComments(await fetchExhibitionComments(exhibitionId));
  }, [exhibitionId]);

  const myId = viewer ? String(viewer.id) : null;
  const isAdmin = viewer?.memberRole === 'ADMIN';

  return (
    <section className="block">
      <h3 className="block-title">댓글 {countComments(comments) || ''}</h3>
      <p className="form-hint" style={{ marginTop: 0, marginBottom: '0.875rem' }}>
        {viewer
          ? `${viewer.name} 님으로 작성됩니다. 댓글은 전시 페이지에 공개돼요.`
          : '댓글은 전시 페이지에 공개돼요.'}
      </p>

      <CommentThread
        comments={comments}
        canWrite={Boolean(viewer)}
        // 서버도 작성자와 관리자만 통과시킨다(403). 화면은 같은 기준으로 버튼을 감출 뿐이다
        canEdit={(comment) => Boolean(isAdmin || (myId && comment.authorId === myId))}
        onCreate={(content, parentId) =>
          createExhibitionComment(exhibitionId, { content, parentId })
        }
        onUpdate={(commentId, content) =>
          updateExhibitionComment(exhibitionId, commentId, content)
        }
        onDelete={(commentId) => deleteExhibitionComment(exhibitionId, commentId)}
        onRefresh={refresh}
      />
    </section>
  );
}
