'use client';

import Link from 'next/link';
import { Tag, TagRow } from '@/components/ui/Tag';
import type { BookmarkItem, TargetType } from '@/lib/types';

const TARGET_LABELS: Record<TargetType, string> = {
  PARTY: '파티',
  CONTEST: '대회',
  GOAL: '전시',
};

const TARGET_ROUTES: Record<TargetType, (id: string) => string> = {
  PARTY: (id) => `/party/${id}`,
  CONTEST: (id) => `/contests/${id}`,
  GOAL: (id) => `/exhibition/${id}`,
};

/**
 * 북마크함 — 파티·대회·성취를 대상 구분 없이 한 목록에 카드형으로 표시한다 (기획서 2.11).
 */
export function BookmarkList({ bookmarks }: { bookmarks: BookmarkItem[] }) {
  if (bookmarks.length === 0) {
    return <p className="notif-empty">아직 북마크한 항목이 없어요.</p>;
  }

  return (
    <div className="board-grid">
      {bookmarks.map((bookmark) => (
        <Link
          key={bookmark.id}
          href={TARGET_ROUTES[bookmark.targetType](bookmark.targetId)}
          className="pboard-card"
        >
          <div className="pboard-top">
            <TagRow>
              <Tag accent>{TARGET_LABELS[bookmark.targetType]}</Tag>
              {bookmark.tags.map((tag) => (
                <Tag key={tag}>{tag}</Tag>
              ))}
            </TagRow>
            <span className="dday">{bookmark.meta}</span>
          </div>

          <h3 className="pboard-title">{bookmark.title}</h3>
          <p className="pboard-meta">{bookmark.subtitle}</p>

          <div className="pboard-footer">
            <span className="contest-host">{bookmark.createdAt} 저장</span>
            <span className="card-link">자세히 보기 →</span>
          </div>
        </Link>
      ))}
    </div>
  );
}
