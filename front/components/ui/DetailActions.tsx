'use client';

import { BookmarkButton } from './BookmarkButton';
import { LikeButton } from './LikeButton';
import {
  toggleContestBookmark,
  toggleContestLike,
  toggleExhibitionBookmark,
  toggleExhibitionLike,
  togglePartyBookmark,
  togglePartyLike,
} from '@/lib/api';

interface DetailActionsProps {
  target: 'party' | 'contest' | 'exhibition';
  id: string;
  likeCount: number;
  likedByMe?: boolean;
  bookmarkedByMe?: boolean;
}

/**
 * 상세 페이지의 좋아요 · 북마크 묶음.
 * 파티 · 대회 · 전시가 같은 모양과 동작을 쓰도록 한곳에서 관리한다 (기획서 3.2).
 */
export function DetailActions({
  target,
  id,
  likeCount,
  likedByMe,
  bookmarkedByMe,
}: DetailActionsProps) {
  const like = (liked: boolean) => {
    if (target === 'party') return togglePartyLike(id, liked);
    if (target === 'contest') return toggleContestLike(id, liked);
    return toggleExhibitionLike(id, liked).then((result) => ({ likeCount: result.likes }));
  };

  const bookmark = (bookmarked: boolean) => {
    if (target === 'party') return togglePartyBookmark(id, bookmarked);
    if (target === 'contest') return toggleContestBookmark(id, bookmarked);
    return toggleExhibitionBookmark(id, bookmarked);
  };

  return (
    <>
      <LikeButton initialCount={likeCount} initialLiked={likedByMe} onToggle={like} />
      <BookmarkButton initialBookmarked={bookmarkedByMe} onToggle={bookmark} />
    </>
  );
}
