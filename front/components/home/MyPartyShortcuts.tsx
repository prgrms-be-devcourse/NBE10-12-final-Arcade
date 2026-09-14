import Link from 'next/link';
import { PARTY_STATUS_LABELS, POSITION_LABELS, TOPIC_TYPE_LABELS } from '@/lib/constants';
import type { MyParty } from '@/lib/types';

export function MyPartyShortcuts({ parties }: { parties: MyParty[] }) {
  const activeParties = parties.filter((party) => party.status === 'IN_PROGRESS');
  if (activeParties.length === 0) return null;
  return <section className="section my-party-shortcuts container" data-reveal>
    <div className="section-head"><h2>내 파티 바로가기</h2><Link className="card-link" href="/mypage">전체 내역 보기 →</Link></div>
    <div className="my-party-row">{activeParties.slice(0, 3).map((party) => <Link className="my-party-card" href={`/party/${party.id}`} key={party.id}>
      <div className="my-party-card-top"><span className="tag">{TOPIC_TYPE_LABELS[party.topicType]}</span><span className="status-pill active">{PARTY_STATUS_LABELS[party.status]}</span></div>
      <h3>{party.name}</h3>
      <p>{party.role === 'OWNER' ? '파티장' : POSITION_LABELS[party.position ?? 'BACK']} · {party.period}</p>
    </Link>)}</div>
  </section>;
}
