'use client';

import { useState } from 'react';
import {
  isServerPartyId,
  parsePartyPullRequest,
  parsePartyPullRequests,
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

/**
 * 파티 저장소에서 동기화된 PR 목록.
 *
 * 서버가 웹훅으로 받아 쌓아둔 값을 그대로 보여준다. 작성자는 GitHub 로그인명이고
 * 아직 크루온 회원과 연결되지 않는다(프로필의 githubUsername 과 맞추는 건 다음 작업).
 */
export function PullRequestList({
  partyId,
  pullRequests: initial,
}: {
  partyId: string;
  pullRequests: PartyPullRequest[];
}) {
  const [pullRequests, setPullRequests] = useState(initial);

  // 웹훅이 PR을 받으면 서버가 곧바로 밀어준다. 목 슬러그 파티는 서버 경로가 없어 구독하지 않는다.
  useServerEvents(isServerPartyId(partyId) ? `/parties/${partyId}/pull-requests/stream` : null, {
    // 연결·재연결 때마다 서버가 현재 목록 전체를 먼저 보낸다. 끊긴 동안의 빈 구간이 여기서 메꿔진다.
    snapshot: (data) => setPullRequests(parsePartyPullRequests(data)),
    'pull-request': (data) => {
      const incoming = parsePartyPullRequest(data);
      setPullRequests((prev) => {
        const index = prev.findIndex((pr) => pr.id === incoming.id);
        if (index < 0) return [incoming, ...prev];
        // 같은 PR의 갱신(리뷰·머지 등)이라 자리를 옮기지 않고 내용만 바꾼다
        const next = [...prev];
        next[index] = incoming;
        return next;
      });
    },
  });

  if (pullRequests.length === 0) {
    return (
      <p className="goal-empty">
        아직 동기화된 PR이 없어요. 저장소를 연결하고 PR을 올리면 여기에 쌓입니다.
      </p>
    );
  }

  return (
    <ul className="pr-list">
      {pullRequests.map((pr) => {
        const badge = badgeOf(pr);
        return (
          <li key={pr.id} className="pr-item">
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
      })}
    </ul>
  );
}
