'use client';

import { useMemo, useState } from 'react';
import {
  ApiError, fetchMyGithubPullRequests, groupLabel, isServerPartyId,
  parsePartyPrGroup, parsePartyPrGroups, replacePrGroup,
  type PartyPrGroup, type PartyPullRequest,
} from '@/lib/api';
import { useServerEvents } from '@/lib/hooks/useServerEvents';
import { Pagination } from '@/components/ui/Pagination';

const PRS_PER_PAGE = 10;

function badgeOf(pr: PartyPullRequest): { label: string; tone: string } {
  if (pr.merged) return { label: '머지됨', tone: 'merged' };
  if (pr.state === 'closed') return { label: '닫힘', tone: 'closed' };
  if (pr.draft) return { label: '초안', tone: 'draft' };
  return { label: '열림', tone: 'open' };
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  // Node의 ICU와 브라우저의 ICU가 ko-KR 오전/오후 표기를 다르게 만들 수 있다.
  // 숫자 부분만 고정 시간대에서 추출하고 문구는 직접 조합해 hydration 결과를 일치시킨다.
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(date);
  const values = Object.fromEntries(parts.filter((part) => part.type !== 'literal').map((part) => [part.type, part.value]));
  return `${values.month}월 ${values.day}일 ${values.hour}:${values.minute}`;
}

function timeLine(pr: PartyPullRequest): string {
  if (pr.mergedAt) return `${formatDateTime(pr.mergedAt)} 머지`;
  if (pr.closedAt) return `${formatDateTime(pr.closedAt)} 닫힘`;
  return `${formatDateTime(pr.openedAt)} 등록`;
}

function PullRequestRow({ pr }: { pr: PartyPullRequest }) {
  const badge = badgeOf(pr);
  return <li className="pr-item">
    <div className="pr-top"><span className="pr-badge" data-tone={badge.tone}>{badge.label}</span><a className="pr-title" href={pr.htmlUrl} target="_blank" rel="noopener noreferrer">{pr.title} ↗</a></div>
    <p className="pr-meta"><span className="pr-number">#{pr.number}</span><span>{pr.authorLogin}</span><span className="pr-branch">{pr.headBranch} → {pr.baseBranch}</span><span>{timeLine(pr)}</span></p>
  </li>;
}

/** 담당자별 PR 묶음을 실시간 표시하고 필요하면 현재 사용자의 PR만 거른다. */
export function PullRequestList({ partyId, groups: initial, liveSyncEnabled }: { partyId: string; groups: PartyPrGroup[]; liveSyncEnabled: boolean }) {
  const [groups, setGroups] = useState(initial);
  const [onlyMine, setOnlyMine] = useState(false);
  const [myIds, setMyIds] = useState<Set<string> | null>(null);
  const [myPrError, setMyPrError] = useState('');
  const [page, setPage] = useState(1);

  // 연동 버튼을 누르기 전에 스트림을 열어 둬야, 연결 과정에서 동기화되는 기존 PR 이벤트를 놓치지 않는다.
  // 스트림의 snapshot은 연결 전에도 빈 목록을 안전하게 반환한다.
  useServerEvents(isServerPartyId(partyId) ? `/parties/${partyId}/pull-requests/grouped-by-member/stream` : null, {
    snapshot: (data) => setGroups(parsePartyPrGroups(data)),
    'pull-request-group': (data) => setGroups((previous) => replacePrGroup(previous, parsePartyPrGroup(data))),
  });

  const visibleGroups = useMemo(() => {
    if (!onlyMine || !myIds) return groups;
    return groups.map((group) => ({ ...group, pullRequests: group.pullRequests.filter((pr) => myIds.has(pr.id)) })).filter((group) => group.pullRequests.length > 0);
  }, [groups, myIds, onlyMine]);
  const flattenedPullRequests = useMemo(
    () => visibleGroups.flatMap((group) => group.pullRequests.map((pr) => ({ group, pr }))),
    [visibleGroups],
  );
  const total = flattenedPullRequests.length;
  const totalPages = Math.ceil(total / PRS_PER_PAGE);
  // 실시간 이벤트로 마지막 페이지의 항목이 사라져도, 렌더 시 유효한 마지막 페이지를 사용한다.
  const activePage = Math.min(page, Math.max(totalPages, 1));
  const pagedGroups = useMemo(() => {
    const entries = flattenedPullRequests.slice((activePage - 1) * PRS_PER_PAGE, activePage * PRS_PER_PAGE);
    const byMember = new Map<string, { group: PartyPrGroup; pullRequests: PartyPullRequest[] }>();
    entries.forEach(({ group, pr }) => {
      const key = group.memberId ?? group.githubLogin ?? 'unknown';
      const existing = byMember.get(key);
      if (existing) existing.pullRequests.push(pr);
      else byMember.set(key, { group, pullRequests: [pr] });
    });
    return [...byMember.values()];
  }, [activePage, flattenedPullRequests]);

  const toggleMine = async () => {
    if (onlyMine) { setOnlyMine(false); setPage(1); return; }
    try {
      const mine = await fetchMyGithubPullRequests();
      setMyIds(new Set(mine.map((pr) => pr.id)));
      setMyPrError('');
      setOnlyMine(true);
      setPage(1);
    } catch (error) {
      setMyPrError(error instanceof ApiError && error.message === 'GITHUB_SOCIAL_LOGIN_REQUIRED'
        ? '내 PR을 보려면 GitHub 소셜 로그인 또는 계정 연결이 필요해요.'
        : '내 PR 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.');
    }
  };

  return <>
    <div className="pr-toolbar">
      <button type="button" className="btn btn-ghost pr-mine-toggle" aria-pressed={onlyMine} onClick={toggleMine}>{onlyMine ? '전체 PR 보기' : '내 PR만 보기'}</button>
      {liveSyncEnabled ? <span className="pr-stream" data-state="connected">실시간 연동 중</span> : null}
    </div>
    {myPrError ? <p className="form-error">{myPrError}</p> : null}
    {total === 0 ? <p className="goal-empty">{onlyMine ? '이 Party에서 작성한 PR이 없어요.' : '아직 동기화된 PR이 없어요. 저장소를 연결하고 PR을 올리면 여기에 쌓입니다.'}</p> :
      <div className="pr-groups">{pagedGroups.map(({ group, pullRequests }) => <section key={group.memberId ?? group.githubLogin ?? 'unknown'} className="pr-group">
        <div className="position-group-head"><h4>{groupLabel(group)}{group.owner ? ' · 파티장' : ''}</h4><span className="frac">PR {group.pullRequests.length}건</span></div>
        <ul className="pr-list">{pullRequests.map((pr) => <PullRequestRow key={pr.id} pr={pr} />)}</ul>
      </section>)}</div>
    }
    <Pagination page={activePage} totalPages={totalPages} onChange={setPage} />
  </>;
}
