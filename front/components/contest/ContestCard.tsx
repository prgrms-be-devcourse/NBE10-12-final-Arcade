import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { CONTEST_FORMAT_LABELS } from '@/lib/constants';
import type { Contest } from '@/lib/types';

/** 공모전 카드 — 홈 가로 스크롤 · 공모전 허브 그리드에서 공용 */
export function ContestCard({ contest }: { contest: Contest }) {
  return (
    <Link href={`/contests/${contest.id}`} className="contest-card">
      <div
        className={contest.coverImageUrl ? 'contest-poster has-cover' : 'contest-poster'}
        style={
          contest.coverImageUrl ? { backgroundImage: `url(${contest.coverImageUrl})` } : undefined
        }
      >
        <span className="cat">
          {CONTEST_FORMAT_LABELS[contest.format]} · {contest.tag}
        </span>
      </div>
      <div className="contest-body">
        <h3>{contest.title}</h3>
        <p className="contest-host">
          {contest.host} · {contest.dday}
        </p>
        <div className="contest-stats">
          <span className="prize">{contest.prize}</span>
          <span className="view-count">
            <Icon name="i-eye" />
            {contest.viewCount.toLocaleString()}
          </span>
          <span className="teams">참가팀 {contest.teams}</span>
        </div>
      </div>
    </Link>
  );
}
