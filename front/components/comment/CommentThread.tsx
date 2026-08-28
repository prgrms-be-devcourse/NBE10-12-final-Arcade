'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import type { ThreadComment } from '@/lib/types';

interface CommentThreadProps {
  comments: ThreadComment[];
  currentUser: string;
  onChange: (comments: ThreadComment[]) => void;
  /** parentId 가 있으면 답글로 생성한다 */
  onCreate: (content: string, parentId?: string) => Promise<ThreadComment>;
  onUpdate: (commentId: string, content: string) => Promise<void>;
  onDelete: (commentId: string) => Promise<void>;
}

/**
 * 댓글 스레드 (기획서 2.7, 3.8).
 * 원댓글에만 답글을 달 수 있어 깊이가 1단계로 제한된다.
 * 대상(현재는 커밋)에 따라 저장 API 만 주입받고 화면 동작은 공통이다.
 */
export function CommentThread({
  comments,
  currentUser,
  onChange,
  onCreate,
  onUpdate,
  onDelete,
}: CommentThreadProps) {
  const { confirm, dialog } = useConfirm();
  const [draft, setDraft] = useState('');
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyDraft, setReplyDraft] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editDraft, setEditDraft] = useState('');

  /** 원댓글 · 답글을 함께 훑어 하나를 갈아끼우거나 지운다 */
  const mapComments = (
    fn: (comment: ThreadComment, isReply: boolean) => ThreadComment | null,
  ): ThreadComment[] =>
    comments
      .map((comment) => {
        const next = fn(comment, false);
        if (!next) return null;
        return {
          ...next,
          replies: next.replies
            .map((reply) => fn(reply, true))
            .filter(Boolean) as ThreadComment[],
        };
      })
      .filter(Boolean) as ThreadComment[];

  const addComment = async () => {
    const content = draft.trim();
    if (!content) return;
    const created = await onCreate(content);
    onChange([...comments, created]);
    setDraft('');
  };

  const addReply = async (parentId: string) => {
    const content = replyDraft.trim();
    if (!content) return;
    const created = await onCreate(content, parentId);
    onChange(
      comments.map((comment) =>
        comment.id === parentId ? { ...comment, replies: [...comment.replies, created] } : comment,
      ),
    );
    setReplyingTo(null);
    setReplyDraft('');
  };

  const saveEdit = async (commentId: string) => {
    const content = editDraft.trim();
    if (!content) return;
    onChange(
      mapComments((comment) =>
        comment.id === commentId ? { ...comment, content, createdAt: '방금 수정' } : comment,
      ),
    );
    setEditingId(null);
    setEditDraft('');
    await onUpdate(commentId, content);
  };

  const remove = async (commentId: string) => {
    const ok = await confirm({
      title: '댓글을 삭제할까요?',
      description: '달린 답글도 함께 사라져요.',
    });
    if (!ok) return;

    onChange(mapComments((comment) => (comment.id === commentId ? null : comment)));
    await onDelete(commentId);
  };

  const renderComment = (comment: ThreadComment, isReply: boolean) => {
    const mine = comment.authorName === currentUser;
    return (
      <div key={comment.id} className="ci-comment">
        <span className="mini-avatar">{comment.authorInitial}</span>
        <div className="ci-comment-body">
          <div className="ci-comment-top">
            <span className="ci-comment-name">{comment.authorName}</span>
            <span className="ci-comment-time">{comment.createdAt}</span>
          </div>
          <p className="ci-comment-text">{comment.content}</p>

          {editingId === comment.id ? (
            <div className="ci-comment-edit">
              <input
                value={editDraft}
                onChange={(event) => setEditDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') saveEdit(comment.id);
                }}
                autoFocus
              />
              <button type="button" className="btn-mini" onClick={() => saveEdit(comment.id)}>
                저장
              </button>
            </div>
          ) : null}

          {mine ? (
            <div className="ci-comment-actions">
              <button
                type="button"
                onClick={() => {
                  setEditingId(editingId === comment.id ? null : comment.id);
                  setEditDraft(comment.content);
                }}
              >
                수정
              </button>
              <button type="button" className="danger" onClick={() => remove(comment.id)}>
                삭제
              </button>
            </div>
          ) : null}

          {/* 답글에는 답글 버튼을 두지 않아 깊이가 1단계로 제한된다 */}
          {!isReply ? (
            <>
              <button
                type="button"
                className="ci-reply-btn"
                onClick={() => {
                  setReplyingTo(replyingTo === comment.id ? null : comment.id);
                  setReplyDraft('');
                }}
              >
                답글
              </button>

              {comment.replies.length > 0 ? (
                <div className="ci-replies">
                  {comment.replies.map((reply) => renderComment(reply, true))}
                </div>
              ) : null}

              {replyingTo === comment.id ? (
                <div className="ci-write">
                  <input
                    placeholder="답글 남기기"
                    value={replyDraft}
                    onChange={(event) => setReplyDraft(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') addReply(comment.id);
                    }}
                    autoFocus
                  />
                  <button type="button" className="btn-mini" onClick={() => addReply(comment.id)}>
                    답글
                  </button>
                </div>
              ) : null}
            </>
          ) : null}
        </div>
      </div>
    );
  };

  return (
    <div className="ci-thread">
      {dialog}
      {comments.length > 0 ? (
        comments.map((comment) => renderComment(comment, false))
      ) : (
        <p style={{ fontSize: '.8rem', color: 'var(--text-dim)' }}>
          아직 댓글이 없어요. 진행 중 막힌 부분을 남겨보세요.
        </p>
      )}

      <div className="ci-write">
        <input
          placeholder="이 항목에 댓글 남기기"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') addComment();
          }}
        />
        <button type="button" className="btn-mini" onClick={addComment}>
          등록
        </button>
      </div>
    </div>
  );
}

/** 댓글 수 — 원댓글 + 답글 합계 */
export function countComments(comments: ThreadComment[]) {
  return comments.reduce((sum, comment) => sum + 1 + comment.replies.length, 0);
}

/** 댓글 토글 버튼 */
export function CommentToggleButton({
  count,
  open,
  onToggle,
}: {
  count: number;
  open: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      className={count > 0 || open ? 'ci-comment-btn has' : 'ci-comment-btn'}
      onClick={onToggle}
    >
      <Icon name="i-comment" />
      {count > 0 ? `댓글 ${count}` : '댓글'}
    </button>
  );
}
