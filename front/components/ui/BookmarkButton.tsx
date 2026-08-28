'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';

interface BookmarkButtonProps {
  initialBookmarked?: boolean;
  /** 켜짐/꺼짐을 서버에 반영한다 */
  onToggle: (bookmarked: boolean) => Promise<unknown>;
}

/**
 * 북마크 토글 — 파티 · 대회 · 전시 상세에서 공용으로 쓴다 (기획서 2.11, 3.2).
 * 누르면 켜지고 다시 누르면 꺼지는 토글이라, 실패하면 이전 상태로 되돌린다.
 */
export function BookmarkButton({ initialBookmarked = false, onToggle }: BookmarkButtonProps) {
  const [bookmarked, setBookmarked] = useState(initialBookmarked);
  const [pending, setPending] = useState(false);

  const toggle = async () => {
    if (pending) return;
    const next = !bookmarked;
    setBookmarked(next);
    setPending(true);
    try {
      await onToggle(next);
    } catch {
      setBookmarked(!next);
    } finally {
      setPending(false);
    }
  };

  return (
    <button
      type="button"
      className={bookmarked ? 'bookmark-btn is-active' : 'bookmark-btn'}
      aria-pressed={bookmarked}
      onClick={toggle}
    >
      <Icon name="i-bookmark" />
      <span className="bookmark-label">{bookmarked ? '북마크됨' : '북마크'}</span>
    </button>
  );
}
