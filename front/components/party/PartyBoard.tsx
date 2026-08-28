'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { PartyCard } from '@/components/party/PartyCard';
import { SelectField } from '@/components/ui/Field';
import { DDay, Tag } from '@/components/ui/Tag';
import { comparePartiesBy } from '@/lib/api';
import {
  PARTY_FIELDS,
  POSITION_LABELS,
  POSITION_TYPES,
  TOPIC_TYPES,
  TOPIC_TYPE_LABELS,
} from '@/lib/constants';
import type { Party, PositionType, TopicType } from '@/lib/types';

const SORT_LABELS = {
  empty: '빈 자리 많은순',
  dday: '마감 임박순',
  like: '인기순',
} as const;

interface PartyBoardProps {
  parties: Party[];
  recommended: (Party & { why: string })[];
  keywords: string;
}

/**
 * 파티 게시판 (검색 · 유형 · 분야 · 정렬).
 * 목 데이터에서는 클라이언트에서 필터링하지만,
 * 실제 API가 붙으면 fetchParties(query) 로 그대로 넘기면 된다.
 */
export function PartyBoard({ parties, recommended, keywords }: PartyBoardProps) {
  const [search, setSearch] = useState('');
  const [topicType, setTopicType] = useState<TopicType | '전체'>('전체');
  const [position, setPosition] = useState<PositionType | '전체'>('전체');
  const [subCategory, setSubCategory] = useState('전체');
  const [sort, setSort] = useState<'empty' | 'dday' | 'like'>('empty');

  const visible = useMemo(() => {
    const filtered = parties.filter(
      (party) =>
        (!search || party.title.includes(search)) &&
        (topicType === '전체' || party.topicType === topicType) &&
        (subCategory === '전체' || party.subCategory === subCategory) &&
        (position === '전체' || party.positions.some((slot) => slot.type === position)),
    );
    return [...filtered].sort(comparePartiesBy(sort));
  }, [parties, search, topicType, subCategory, position, sort]);

  return (
    <>
      <section className="reco-row" data-reveal>
        <div className="reco-head">
          <span className="reco-tag">FOR YOU</span>
          <p className="reco-desc">성취 키워드 &quot;{keywords}&quot;와 잘 맞는 파티예요.</p>
        </div>
        <div className="reco-cards">
          {recommended.map((party) => (
            <Link key={party.id} href={`/party/${party.id}`} className="reco-card">
              <div className="reco-top">
                <Tag accent>키워드 일치 {party.matchScore ?? 0}%</Tag>
                <DDay>{party.dday}</DDay>
              </div>
              <h4>{party.title}</h4>
              <p className="reco-why">{party.why}</p>
              <div className="position-slots">
                {party.positions.slice(0, 1).map((slot) => (
                  <span key={slot.type} className="slot-chip">
                    {POSITION_LABELS[slot.type]} {slot.filledCount}/{slot.capacity}
                  </span>
                ))}
              </div>
            </Link>
          ))}
        </div>
      </section>

      <div className="party-toolbar">
        <div className="search-field">
          <Icon name="i-search" className="search-icon" />
          <input
            type="text"
            placeholder="파티 제목으로 검색"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <SelectField
          className="select-field"
          value={topicType}
          onChange={(event) => setTopicType(event.target.value as TopicType | '전체')}
          aria-label="주제 유형"
        >
          <option value="전체">유형 전체</option>
          {TOPIC_TYPES.map((type) => (
            <option key={type} value={type}>
              {TOPIC_TYPE_LABELS[type]}
            </option>
          ))}
        </SelectField>
        <SelectField
          className="select-field"
          value={position}
          onChange={(event) => setPosition(event.target.value as PositionType | '전체')}
          aria-label="포지션"
        >
          <option value="전체">포지션 전체</option>
          {POSITION_TYPES.map((type) => (
            <option key={type} value={type}>
              {POSITION_LABELS[type]}
            </option>
          ))}
        </SelectField>
        <SelectField
          className="select-field"
          value={subCategory}
          onChange={(event) => setSubCategory(event.target.value)}
          aria-label="분야"
        >
          <option value="전체">분야 전체</option>
          {PARTY_FIELDS.map((field) => (
            <option key={field} value={field}>
              {field}
            </option>
          ))}
        </SelectField>
        <SelectField
          className="select-field"
          value={sort}
          onChange={(event) => setSort(event.target.value as 'empty' | 'dday' | 'like')}
          aria-label="정렬"
        >
          {Object.entries(SORT_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
      </div>

      <div className="board-grid">
        {visible.map((party) => (
          <PartyCard key={party.id} party={party} />
        ))}
      </div>
      {visible.length === 0 ? <p className="notif-empty">조건에 맞는 파티가 없어요.</p> : null}
    </>
  );
}
