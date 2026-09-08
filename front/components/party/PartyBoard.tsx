'use client';

import { type FormEvent, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { PartyCard } from '@/components/party/PartyCard';
import { SelectField } from '@/components/ui/Field';
import { DDay, Tag } from '@/components/ui/Tag';
import { comparePartiesBy, fetchPartySearch, type PartySearchFilters } from '@/lib/api';
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

/** 파티 게시판 (검색 · 유형 · 분야 · 정렬). */
export function PartyBoard({ parties, recommended, keywords }: PartyBoardProps) {
  const [draft, setDraft] = useState('');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Party[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchFailed, setSearchFailed] = useState(false);
  const [topicType, setTopicType] = useState<TopicType | '전체'>('전체');
  const [position, setPosition] = useState<PositionType | '전체'>('전체');
  const [subCategory, setSubCategory] = useState('전체');
  const [sort, setSort] = useState<'empty' | 'dday' | 'like'>('empty');
  const searchSeq = useRef(0);

  // 분야·유형·포지션 필터링은 /parties/search 가 처리한다.
  // 필터를 바꿀 때마다 현재 검색어로 서버 검색을 다시 실행한다.
  const runSearch = async (raw: string, filters: PartySearchFilters = {}) => {
    const next = raw.trim();

    if (!next) {
      searchSeq.current += 1;
      setResults(null);
      setSearching(false);
      setSearchFailed(false);
      return;
    }

    const seq = (searchSeq.current += 1);
    setSearching(true);
    setSearchFailed(false);
    try {
      const found = await fetchPartySearch(next, { size: 100, ...filters });
      if (seq === searchSeq.current) setResults(found);
    } catch (error) {
      // fetchPartySearch 는 결과 없음을 빈 배열로 돌려준다. 여기까지 온 건 네트워크·서버
      // 오류라, 직전 목록을 지우지 않고 알림만 띄운다.
      if (seq === searchSeq.current) {
        console.error('파티 검색 요청 실패', error);
        setSearchFailed(true);
      }
    } finally {
      if (seq === searchSeq.current) setSearching(false);
    }
  };

  const applyFilters = (raw: string, patch?: Partial<PartySearchFilters>) => {
    void runSearch(raw, { topicType, position, subCategory, ...patch });
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const next = draft.trim();
    setQuery(next);
    applyFilters(next);
  };

  const visible = useMemo(() => {
    // 검색 결과에도 필터를 걸어, 서버 왕복 전 이전 결과가 새 필터 기준으로 좁혀지게 한다.
    const matchesFilters = (party: Party) =>
      (topicType === '전체' || party.topicType === topicType) &&
      (subCategory === '전체' || party.subCategory === subCategory) &&
      (position === '전체' || party.positions.some((slot) => slot.type === position));

    const base = query ? (results ?? []) : parties;
    return [...base.filter(matchesFilters)].sort(comparePartiesBy(sort));
  }, [parties, results, query, topicType, subCategory, position, sort]);

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
                {party.positions.slice(0, 1).map((slot, index) => (
                  <span key={`${slot.type}-${index}`} className="slot-chip">
                    {POSITION_LABELS[slot.type]} {slot.filledCount}/{slot.capacity}
                  </span>
                ))}
              </div>
            </Link>
          ))}
        </div>
      </section>

      <div className="party-toolbar">
        <form className="search-field" role="search" onSubmit={handleSearch}>
          <button type="submit" className="search-icon" aria-label="검색">
            <Icon name="i-search" />
          </button>
          <input
            type="text"
            placeholder="파티 검색"
            maxLength={25}
            value={draft}
            onChange={(event) => {
              const next = event.target.value;
              setDraft(next);
              if (next === '') {
                setQuery('');
                applyFilters('');
              }
            }}
          />
        </form>
        <SelectField
          className="select-field"
          value={topicType}
          onChange={(event) => {
            const next = event.target.value as TopicType | '전체';
            setTopicType(next);
            applyFilters(query, { topicType: next });
          }}
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
          onChange={(event) => {
            const next = event.target.value as PositionType | '전체';
            setPosition(next);
            applyFilters(query, { position: next });
          }}
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
          onChange={(event) => {
            const next = event.target.value;
            setSubCategory(next);
            applyFilters(query, { subCategory: next });
          }}
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
      {searching ? (
        <p className="notif-empty">검색 중…</p>
      ) : searchFailed ? (
        <p className="notif-empty">검색 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.</p>
      ) : visible.length === 0 ? (
        <p className="notif-empty">
          {query ? `'${query}' 검색 결과가 없어요.` : '조건에 맞는 파티가 없어요.'}
        </p>
      ) : null}
    </>
  );
}
