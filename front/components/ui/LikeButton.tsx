'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';

interface LikeButtonProps {
  initialCount: number;
  initialLiked?: boolean;
  /** 눌린 상태를 서버에 반영한다 — 갱신된 좋아요 수를 돌려준다 */
  onToggle: (liked: boolean) => Promise<{ likeCount: number }>;
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
    setLiked(next);
    setCount((value) => value + (next ? 1 : -1));
    setPending(true);
    try {
      const result = await onToggle(next);
      setCount(result.likeCount);
    } catch {
      setLiked(!next);
      setCount((value) => value + (next ? -1 : 1));
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
