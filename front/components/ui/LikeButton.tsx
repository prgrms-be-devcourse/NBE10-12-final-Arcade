'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';

interface LikeButtonProps {
  initialCount: number;
  initialLiked?: boolean;
  /**
   * 눌린 상태를 서버에 반영한다 — 갱신된 좋아요 수를 돌려준다.
   * 취소 응답에 수가 없는 대상(전시)을 위해 화면이 이미 반영한 수를 함께 넘긴다.
   */
  onToggle: (liked: boolean, currentCount: number) => Promise<{ likeCount: number }>;
  label?: string;
}

/**
 * 좋아요 토글 — 파티 · 대회 공용.
 * 누르면 켜지고 다시 누르면 꺼지는 토글이며, 응답의 likeCount 로 화면을 맞춘다 (기획서 3.2).
 */
export function LikeButton({
  initialCount,
  initialLiked = false,
  onToggle,
  label = '좋아요',
}: LikeButtonProps) {
  const [liked, setLiked] = useState(initialLiked);
  const [count, setCount] = useState(initialCount);
  const [pending, setPending] = useState(false);

  const toggle = async () => {
    if (pending) return;
    const next = !liked;
    // 낙관적 반영 후 서버 응답으로 확정한다
    const optimistic = count + (next ? 1 : -1);
    setLiked(next);
    setCount(optimistic);
    setPending(true);
    try {
      const result = await onToggle(next, optimistic);
      setCount(result.likeCount);
    } catch {
      setLiked(!next);
      setCount(count);
    } finally {
      setPending(false);
    }
  };

  return (
    <button
      type="button"
      className={liked ? 'bookmark-btn is-active' : 'bookmark-btn'}
      aria-pressed={liked}
      onClick={toggle}
    >
      <Icon name="i-heart" />
      <span className="bookmark-label">
        {label} {count}
      </span>
    </button>
  );
}
