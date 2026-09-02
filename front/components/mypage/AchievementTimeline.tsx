'use client';

import { useMemo, useState } from 'react';
import { FilterBlock, FilterChips } from '@/components/ui/FilterChips';
import { SelectField } from '@/components/ui/Field';
import { ChipRow, SkillChip, SourceBadge } from '@/components/ui/Tag';
import {
  GOAL_SOURCE_LABELS,
  GOAL_STATUSES,
  GOAL_STATUS_LABELS,
  GOAL_TYPES,
  GOAL_TYPE_LABELS,
} from '@/lib/constants';
import type { Achievement } from '@/lib/types';

const STATUS_OPTIONS = ['전체', ...GOAL_STATUSES.map((value) => GOAL_STATUS_LABELS[value])];
const TYPE_OPTIONS = ['전체', ...GOAL_TYPES.map((value) => GOAL_TYPE_LABELS[value])];
const SOURCE_OPTIONS = ['전체', GOAL_SOURCE_LABELS.SELF_REPORTED, GOAL_SOURCE_LABELS.PLATFORM_VERIFIED];

/**
 * 성취 리스트 — 상태 · 유형 · 출처 · 연도로 거르고 연도별로 묶어 보여준다 (기획서 2.11).
 */
export function AchievementTimeline({ achievements }: { achievements: Achievement[] }) {
  const [statusLabel, setStatusLabel] = useState('전체');
  const [typeLabel, setTypeLabel] = useState('전체');
  const [sourceLabel, setSourceLabel] = useState('전체');
  const [year, setYear] = useState('전체');

  const years = useMemo(
    () => Array.from(new Set(achievements.map((item) => item.year))).sort().reverse(),
    [achievements],
  );

  /** 연도 그룹 — 최신 연도가 위로, 그룹 안에서도 최신순 */
  const groups = useMemo(() => {
    const status = GOAL_STATUSES.find((value) => GOAL_STATUS_LABELS[value] === statusLabel);
    const type = GOAL_TYPES.find((value) => GOAL_TYPE_LABELS[value] === typeLabel);

    const visible = achievements
      .filter(
        (achievement) =>
          (statusLabel === '전체' || achievement.status === status) &&
          (typeLabel === '전체' || achievement.type === type) &&
          (sourceLabel === '전체' || GOAL_SOURCE_LABELS[achievement.source] === sourceLabel) &&
          (year === '전체' || achievement.year === year),
      )
      .sort((a, b) => b.period.localeCompare(a.period));

    const map = new Map<string, Achievement[]>();
    visible.forEach((achievement) => {
      map.set(achievement.year, [...(map.get(achievement.year) ?? []), achievement]);
    });
    return Array.from(map.entries()).sort((a, b) => b[0].localeCompare(a[0]));
  }, [achievements, statusLabel, typeLabel, sourceLabel, year]);

  return (
    <>
      <FilterBlock>
        <FilterChips
          label="상태"
          options={STATUS_OPTIONS}
          value={statusLabel}
          onChange={setStatusLabel}
        />
        <FilterChips label="유형" options={TYPE_OPTIONS} value={typeLabel} onChange={setTypeLabel} />
        <FilterChips
          label="출처"
          options={SOURCE_OPTIONS}
          value={sourceLabel}
          onChange={setSourceLabel}
        />
        {/* 상태 · 유형 · 출처 칩 그룹과 같은 줄 구조로 맞춘다 (라벨 + 선택창) */}
        <div className="filter-group">
          <label className="filter-label" htmlFor="achvYearSelect">
            연도
          </label>
          <SelectField
            id="achvYearSelect"
            className="filter-select"
            value={year}
            onChange={(event) => setYear(event.target.value)}
          >
            <option value="전체">전체</option>
            {years.map((value) => (
              <option key={value} value={value}>
                {value}년
              </option>
            ))}
          </SelectField>
        </div>
      </FilterBlock>

      <div className="achv-timeline">
        {groups.map(([groupYear, items]) => (
          <div key={groupYear} className="achv-year">
            <div className="achv-year-head">
              <span className="achv-year-label">{groupYear}</span>
              <span className="achv-year-count">{items.length}건</span>
              <span className="achv-year-rule" />
            </div>

            <div className="achv-track">
              {items.map((achievement) => (
                <div
                  key={achievement.id}
                  className="achv-item"
                  data-status={GOAL_STATUS_LABELS[achievement.status]}
                >
                  <div className="achv-item-top">
                    <span className="achv-date">{achievement.period}</span>
                    <SourceBadge source={achievement.source} />
                    <span
                      className={
                        achievement.status === 'ACHIEVED'
                          ? 'status-badge achieved'
                          : achievement.status === 'IN_PROGRESS'
                            ? 'status-badge progress'
                            : 'status-badge'
                      }
                    >
                      {GOAL_STATUS_LABELS[achievement.status]}
                    </span>
                    <span className="status-badge">{GOAL_TYPE_LABELS[achievement.type]}</span>
                  </div>

                  <h5>{achievement.title}</h5>
                  {achievement.description ? (
                    <p className="achv-sub">{achievement.description}</p>
                  ) : null}

                  {achievement.tags.length > 0 ? (
                    <ChipRow>
                      {achievement.tags.map((tag) => (
                        <SkillChip key={tag}>{tag}</SkillChip>
                      ))}
                    </ChipRow>
                  ) : null}

                  {achievement.links.map((link) => (
                    <a
                      key={link.url}
                      className="card-link"
                      href={link.url}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {link.label} →
                    </a>
                  ))}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {groups.length === 0 ? (
        <p className="achv-empty">
          {achievements.length === 0
            ? '아직 쌓인 성취가 없어요. 대회 수상이나 스스로 정한 목표를 직접 등록해 보세요.'
            : '조건에 맞는 성취가 없어요.'}
        </p>
      ) : null}
    </>
  );
}
