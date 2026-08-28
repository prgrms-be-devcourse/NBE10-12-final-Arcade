'use client';

import { useEffect, useState } from 'react';
import { ContestCard } from './ContestCard';
import { FilterBlock, FilterChips } from '@/components/ui/FilterChips';
import { fetchContests } from '@/lib/api';
import { CONTEST_FORMATS, CONTEST_FORMAT_LABELS, CONTEST_TAGS } from '@/lib/constants';
import type { Contest, ContestFormat, ContestTag } from '@/lib/types';

const FORMAT_OPTIONS = ['전체', ...CONTEST_FORMATS.map((f) => CONTEST_FORMAT_LABELS[f])];
const TAG_OPTIONS = ['전체', ...CONTEST_TAGS];

/**
 * 대회 허브 — 형식(공모전/해커톤)과 분야 두 축으로 필터링한다 (기획서 2.4).
 *
 * 목록도 클라이언트에서 조회한다 — 목 모드에서 방금 등록한 대회가
 * 브라우저 쪽 저장소에만 있어, 서버에서 조회하면 목록에 나타나지 않기 때문이다.
 */
export function ContestBoard() {
  const [contests, setContests] = useState<Contest[]>([]);
  const [loading, setLoading] = useState(true);
  const [formatLabel, setFormatLabel] = useState('전체');
  const [tag, setTag] = useState('전체');

  useEffect(() => {
    let alive = true;
    fetchContests()
      .then((loaded) => {
        if (alive) setContests(loaded);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);

  const format: ContestFormat | '전체' =
    formatLabel === '전체'
      ? '전체'
      : (CONTEST_FORMATS.find((f) => CONTEST_FORMAT_LABELS[f] === formatLabel) ?? '전체');

  const visible = contests.filter(
    (contest) =>
      (format === '전체' || contest.format === format) &&
      (tag === '전체' || contest.tag === (tag as ContestTag)),
  );

  return (
    <>
      <FilterBlock>
        <FilterChips
          label="형식"
          options={FORMAT_OPTIONS}
          value={formatLabel}
          onChange={setFormatLabel}
        />
        <FilterChips label="분야" options={TAG_OPTIONS} value={tag} onChange={setTag} />
      </FilterBlock>

      <div className="board-grid">
        {visible.map((contest) => (
          <ContestCard key={contest.id} contest={contest} />
        ))}
      </div>
      {loading ? (
        <p className="notif-empty">대회 목록을 불러오는 중이에요.</p>
      ) : visible.length === 0 ? (
        <p className="notif-empty">조건에 맞는 대회가 없어요.</p>
      ) : null}
    </>
  );
}
