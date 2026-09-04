import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { DDay, Tag, TagRow } from '@/components/ui/Tag';
import { POSITION_LABELS, TOPIC_TYPE_LABELS } from '@/lib/constants';
import type { Party } from '@/lib/types';

/** 홈 TOP3 시상대 카드 */
export function RankCard({ party, rank }: { party: Party; rank: 1 | 2 | 3 }) {
  return (
    <article className={`rank-card rank-${rank}`}>
      <div className="rank-badge">
        {rank === 1 ? <Icon name="i-crown" className="crown" /> : null}
        <span className="rank-num">{rank}</span>
      </div>

      <h3 className="rank-title">{party.title}</h3>

      <TagRow>
        {party.contestName ? (
          <Tag accent>대회 연동 · {party.contestName}</Tag>
        ) : (
          <Tag>{TOPIC_TYPE_LABELS[party.topicType]}</Tag>
        )}
      </TagRow>

      <TagRow>
        {party.positions.map((position, index) => (
          <Tag key={`${position.type}-${index}`}>
            {POSITION_LABELS[position.type]} {position.capacity}
          </Tag>
        ))}
      </TagRow>

      <div className="meta-row">
        <span className="applicants">
          <Icon name="i-heart" />
          좋아요 {party.likeCount}
        </span>
        <DDay>{party.dday}</DDay>
      </div>

      <Link className="card-link" href={`/party/${party.id}`}>
        자세히 보기 →
      </Link>
    </article>
  );
}
