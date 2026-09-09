'use client';

import { useState } from 'react';
import {
  groupLabel,
  isServerPartyId,
  parsePartyPrGroup,
  parsePartyPrGroups,
  replacePrGroup,
  type PartyPrGroup,
  type PartyPullRequest,
} from '@/lib/api';
import { useServerEvents } from '@/lib/hooks/useServerEvents';

/** PR 한 건의 상태 배지. GitHub 의 open/closed 에 draft·merged 를 얹어 네 가지로 본다 */
function badgeOf(pr: PartyPullRequest): { label: string; tone: string } {
  if (pr.merged) return { label: '머지됨', tone: 'merged' };
  if (pr.state === 'closed') return { label: '닫힘', tone: 'closed' };
  if (pr.draft) return { label: '초안', tone: 'draft' };
  return { label: '열림', tone: 'open' };
}

/** 2026.08.29 11:31 */
function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** PR 한 건의 시각 문구 — 머지·닫힘은 그 시점이, 열려 있으면 연 시점이 기준이다 */
function timeLine(pr: PartyPullRequest): string {
  if (pr.mergedAt) return `${formatDateTime(pr.mergedAt)} 머지`;
  if (pr.closedAt) return `${formatDateTime(pr.closedAt)} 닫힘`;
  return `${formatDateTime(pr.openedAt)} 등록`;
}

function PullRequestRow({ pr }: { pr: PartyPullRequest }) {
  const badge = badgeOf(pr);
  return (
    <li className="pr-item">
      <div className="pr-top">
        <span className="pr-badge" data-tone={badge.tone}>
          {badge.label}
        </span>
        <a className="pr-title" href={pr.htmlUrl} target="_blank" rel="noopener noreferrer">
          {pr.title} ↗
        </a>
      </div>
      <p className="pr-meta">
        <span className="pr-number">#{pr.number}</span>
        <span>{pr.authorLogin}</span>
        <span className="pr-branch">
          {pr.headBranch} → {pr.baseBranch}
        </span>
        <span>{timeLine(pr)}</span>
      </p>
    </li>
  );
}

/**
 * 파티 저장소에서 동기화된 PR 을 담당자별로 묶어 보여준다 (ARC-108).
 *
 * 서버가 PR 작성자의 GitHub 계정 id 와 회원의 연동 계정을 맞춰 묶어주므로, 크루온 회원이면
 * 이름으로 뜬다. 파티원과 연결되지 않은 작성자는 GitHub 로그인명 그대로 남는다 -
 * **파티원이 GitHub 을 연동하지 않았으면 본인 이름 묶음은 0건이고 로그인명 묶음이 따로 생긴다.**
 */
export function PullRequestList({
  partyId,
  groups: initial,
}: {
  partyId: string;
  groups: PartyPrGroup[];
}) {
  const [groups, setGroups] = useState(initial);

  // 웹훅이 PR을 받으면 서버가 곧바로 밀어준다. 목 슬러그 파티는 서버 경로가 없어 구독하지 않는다.
  useServerEvents(
    isServerPartyId(partyId)
      ? `/parties/${partyId}/pull-requests/grouped-by-member/stream`
      : null,
    {
      // 연결·재연결 때마다 서버가 현재 묶음 전체를 먼저 보낸다. 끊긴 동안의 빈 구간이 여기서 메꿔진다.
      snapshot: (data) => setGroups(parsePartyPrGroups(data)),
      // 갱신도 snapshot 과 같은 묶음 형태로 온다 (ARC-130). 바뀐 작성자 묶음만 갈아 끼운다.
      'pull-request-group': (data) =>
        setGroups((prev) => replacePrGroup(prev, parsePartyPrGroup(data))),
    },
  );

  const total = groups.reduce((count, group) => count + group.pullRequests.length, 0);
  if (total === 0) {
    return (
      <p className="goal-empty">
        아직 동기화된 PR이 없어요. 저장소를 연결하고 PR을 올리면 여기에 쌓입니다.
      </p>
    );
  }

  return (
    <div className="pr-groups">
      {groups.map((group) => (
        <section key={group.memberId ?? group.githubLogin ?? 'unknown'} className="pr-group">
          <div className="position-group-head">
            <h4>
              {groupLabel(group)}
              {group.owner ? ' · 파티장' : ''}
            </h4>
            <span className="frac">PR {group.pullRequests.length}건</span>
          </div>
          {group.pullRequests.length > 0 ? (
            <ul className="pr-list">
              {group.pullRequests.map((pr) => (
                <PullRequestRow key={pr.id} pr={pr} />
              ))}
            </ul>
          ) : (
            <p className="checklist-note">아직 올린 PR이 없어요.</p>
          )}
        </section>
      ))}
    </div>
  );
}
