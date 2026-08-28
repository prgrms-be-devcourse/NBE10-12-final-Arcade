import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { LeaderRow } from '@/components/ui/Avatar';
import { DDay, SlotChip, Tag, TagRow } from '@/components/ui/Tag';
import { CONTEST_FORMAT_LABELS, TOPIC_TYPE_LABELS } from '@/lib/constants';
import type { Party } from '@/lib/types';

/** 파티 게시판 카드 */
export function PartyCard({ party }: { party: Party }) {
  return (
    <article className="pboard-card">
      <div className="pboard-top">
        <TagRow>
          <Tag>{TOPIC_TYPE_LABELS[party.topicType]}</Tag>
          {party.contestFormat ? <Tag>{CONTEST_FORMAT_LABELS[party.contestFormat]}</Tag> : null}
          <Tag>{party.subCategory}</Tag>
        </TagRow>
        <DDay>{party.dday}</DDay>
      </div>

      <h3 className="pboard-title">{party.title}</h3>

      <div className="position-slots">
        {party.positions.map((position) => (
          <SlotChip key={position.type} position={position} />
        ))}
      </div>

      <div className="pboard-meta">
        <Icon name="i-users" />
        지원자 {party.applicants}명
        <Icon name="i-heart" style={{ marginLeft: '0.75rem' }} />
        {party.likeCount}
      </div>

      <div className="pboard-footer">
        <LeaderRow user={party.leader} href={`/profile/${party.leader.id}`} />
        <Link className="card-link" href={`/party/${party.id}`}>
          자세히 보기 →
        </Link>
      </div>
    </article>
  );
}
