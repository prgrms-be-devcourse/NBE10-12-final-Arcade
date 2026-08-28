'use client';

import { useState } from 'react';
import { CommentThread } from '@/components/comment/CommentThread';
import {
  createExhibitionComment,
  deleteExhibitionComment,
  updateExhibitionComment,
} from '@/lib/api';
import type { ThreadComment } from '@/lib/types';

interface CommentSectionProps {
  exhibitionId: string;
  comments: ThreadComment[];
  currentUserName: string;
}

/** 전시 상세 댓글 — 1단계 답글과 본인 글 수정·삭제를 지원한다 */
export function CommentSection({
  exhibitionId,
  comments: initialComments,
  currentUserName,
}: CommentSectionProps) {
  const [comments, setComments] = useState(initialComments);

  return (
    <section className="block">
      <h3 className="block-title">댓글</h3>
      <p className="form-hint" style={{ marginTop: 0, marginBottom: '0.875rem' }}>
        {currentUserName} 님으로 작성됩니다. 댓글은 전시 페이지에 공개돼요.
      </p>

      <CommentThread
        comments={comments}
        currentUser={currentUserName}
        onChange={setComments}
        onCreate={(content, parentId) =>
          createExhibitionComment(exhibitionId, { content, parentId })
        }
        onUpdate={(commentId, content) =>
          updateExhibitionComment(exhibitionId, commentId, content)
        }
        onDelete={(commentId) => deleteExhibitionComment(exhibitionId, commentId)}
      />
    </section>
  );
}
