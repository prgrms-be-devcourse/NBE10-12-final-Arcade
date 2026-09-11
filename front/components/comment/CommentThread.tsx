'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { Notice } from '@/components/ui/Notice';
import { ApiError } from '@/lib/api';
import { isEnterCommit } from '@/lib/ime';
import type { ThreadComment } from '@/lib/types';

interface CommentThreadProps {
  comments: ThreadComment[];
  /** 이 댓글을 고치거나 지울 수 있는지. 작성자 본인과 관리자만 true 다 */
  canEdit: (comment: ThreadComment) => boolean;
  /** 로그인해야 쓸 수 있다. false 면 입력칸을 숨긴다 */
  canWrite: boolean;
  /** parentId 가 있으면 답글로 생성한다 */
  onCreate: (content: string, parentId?: string) => Promise<unknown>;
  onUpdate: (commentId: string, content: string) => Promise<void>;
  onDelete: (commentId: string) => Promise<void>;
  /**
   * 변경 뒤 목록을 다시 읽는다.
   *
   * 화면에서 직접 트리를 고치지 않는 이유는 서버가 삭제를 soft delete 로 처리하기 때문이다 -
   * 지운 댓글이 사라지는 게 아니라 '삭제된 댓글' 자리로 남아야 달린 답글이 살아남는다.
   * 다시 읽는 편이 그 규칙을 화면에 옮겨 적는 것보다 정확하다.
   */
  onRefresh: () => Promise<void>;
}

/**
 * 댓글 스레드 (기획서 2.7, 3.8).
 * 원댓글에만 답글을 달 수 있어 깊이가 1단계로 제한된다.
 * 대상(현재는 커밋)에 따라 저장 API 만 주입받고 화면 동작은 공통이다.
 */
export function CommentThread({
  comments,
  canEdit,
  canWrite,
  onCreate,
  onUpdate,
  onDelete,
  onRefresh,
}: CommentThreadProps) {
  const { confirm, dialog } = useConfirm();
  const [draft, setDraft] = useState('');
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyDraft, setReplyDraft] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editDraft, setEditDraft] = useState('');

  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');

  /**
   * 서버에 반영한 뒤 목록을 다시 읽는다.
   *
   * 실패를 삼키면 눌렀는데 아무 일도 안 일어난 것처럼 보인다 - 권한 없음(403)이나
   * 빈 내용(400)처럼 서버가 이유를 주는 경우가 있어 그 문구를 그대로 보여준다.
   */
  const run = async (action: () => Promise<unknown>): Promise<boolean> => {
    if (pending) return false;
    setPending(true);
    setError('');
    try {
      await action();
      await onRefresh();
      return true;
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '처리하지 못했어요. 잠시 후 다시 시도해 주세요.');
      return false;
    } finally {
      setPending(false);
    }
  };

  const addComment = async () => {
    const content = draft.trim();
    if (!content) return;
    if (await run(() => onCreate(content))) setDraft('');
  };

  const addReply = async (parentId: string) => {
    const content = replyDraft.trim();
    if (!content) return;
    if (!(await run(() => onCreate(content, parentId)))) return;
    setReplyDraft('');
    setReplyingTo(null);
  };

  const saveEdit = async (commentId: string) => {
    const content = editDraft.trim();
    if (!content) return;
    if (await run(() => onUpdate(commentId, content))) setEditingId(null);
  };

  const remove = async (commentId: string) => {
    const ok = await confirm({
      title: '댓글을 삭제할까요?',
      description: '달린 답글은 그대로 남고, 이 댓글 자리에는 삭제 안내가 보여요.',
    });
    if (!ok) return;
    await run(() => onDelete(commentId));
  };

  const renderComment = (comment: ThreadComment, isReply: boolean) => {
    // 지워진 댓글은 자리만 남는다 - 고치거나 답글을 달 수 없다
    const editable = !comment.deleted && canEdit(comment);
    return (
      <div key={comment.id} className="ci-comment">
        <span className="mini-avatar">{comment.deleted ? '-' : comment.authorInitial}</span>
        <div className="ci-comment-body">
          <div className="ci-comment-top">
            <span className="ci-comment-name">
              {comment.deleted ? '삭제됨' : comment.authorName}
            </span>
            {!comment.deleted && comment.isPartyMember ? (
              <span className="ci-comment-badge">파티원</span>
            ) : null}
            <span className="ci-comment-time">{comment.createdAt}</span>
          </div>
          {comment.deleted ? (
            <p className="ci-comment-text is-deleted">삭제된 댓글이에요.</p>
          ) : (
            <p className="ci-comment-text">{comment.content}</p>
          )}

          {editingId === comment.id ? (
            <div className="ci-comment-edit">
              <input
                value={editDraft}
                onChange={(event) => setEditDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (isEnterCommit(event)) saveEdit(comment.id);
                }}
                autoFocus
              />
              <button type="button" className="btn-mini" onClick={() => saveEdit(comment.id)}>
                저장
              </button>
            </div>
          ) : null}

          {editable ? (
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
              {canWrite && !comment.deleted ? (
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
              ) : null}

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
                      if (isEnterCommit(event)) addReply(comment.id);
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
      {error ? <Notice tone="error">{error}</Notice> : null}
      {comments.length > 0 ? (
        comments.map((comment) => renderComment(comment, false))
      ) : (
        <p style={{ fontSize: '.8rem', color: 'var(--text-dim)' }}>
          아직 댓글이 없어요. 진행 중 막힌 부분을 남겨보세요.
        </p>
      )}

      {canWrite ? (
        <div className="ci-write">
          <input
            placeholder="댓글 남기기"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (isEnterCommit(event)) addComment();
            }}
            disabled={pending}
          />
          <button type="button" className="btn-mini" onClick={addComment} disabled={pending}>
            {pending ? '등록 중…' : '등록'}
          </button>
        </div>
      ) : (
        <p style={{ fontSize: '.8rem', color: 'var(--text-dim)' }}>
          댓글을 남기려면 로그인해 주세요.
        </p>
      )}
    </div>
  );
}

/** 댓글 수 — 원댓글 + 답글 합계. 지워진 자리는 세지 않는다 */
export function countComments(comments: ThreadComment[]): number {
  return comments.reduce(
    (sum, comment) =>
      sum + (comment.deleted ? 0 : 1) + comment.replies.filter((reply) => !reply.deleted).length,
    0,
  );
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
