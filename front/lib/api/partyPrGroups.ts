/**
 * 담당자별 PR 묶음의 순수 로직.
 *
 * 별도 파일인 이유는 SSE 이벤트를 그룹에 꽂아 넣는 규칙이 눈으로만 봐서는 틀리기 쉬워서다
 * (같은 PR 갱신 · 새 작성자 · 작성자 미상). 의존성이 없어 partyPrGroups.check.ts 로 바로 돌려볼 수 있다.
 */
import type { PartyPullRequest } from './partyGithub';

/**
 * 백엔드 PartyPrByMemberDto — 파티 PR 을 GitHub 계정 단위로 묶은 한 덩어리.
 *
 * memberId 가 있으면 크루온 회원(파티장·승인 파티원)이고, 없으면 파티원과 연결되지 않은
 * 외부 GitHub 작성자다. 회원 그룹은 PR 이 0건이어도 내려온다 — 그래서 팀원 목록으로도 쓴다.
 */
export interface PartyPrGroup {
  memberId?: string;
  memberName?: string;
  /** 연동된 GitHub 계정. 회원이 연동하지 않았거나 작성자를 모르는 묶음이면 없다 */
  githubUserId?: number;
  /** 외부 작성자의 GitHub 로그인명. 회원 그룹에는 서버가 담지 않는다 */
  githubLogin?: string;
  owner: boolean;
  pullRequests: PartyPullRequest[];
}

/**
 * 이 PR 이 이 묶음 것인지 본다.
 *
 * 작성자를 모르는 PR(authorGithubUserId 없음)은 GitHub 미연동 회원 묶음과 헷갈리기 쉽다 —
 * 둘 다 githubUserId 가 비어 있어서다. 그래서 '작성자 미상' 묶음(회원도 아닌 쪽)으로만 보낸다.
 */
function belongsTo(group: PartyPrGroup, pullRequest: PartyPullRequest): boolean {
  if (pullRequest.authorGithubUserId == null) {
    return group.memberId === undefined && group.githubUserId === undefined;
  }
  return group.githubUserId === pullRequest.authorGithubUserId;
}

/**
 * SSE 로 들어온 PR 한 건을 묶음 목록에 반영한다.
 *
 * 1. 이미 있는 PR 이면 그 자리에서 내용만 바꾼다 (리뷰·머지로 순서가 튀지 않게).
 * 2. 새 PR 이면 작성자 묶음 맨 앞에 넣는다.
 * 3. 처음 보는 작성자면 외부 작성자 묶음을 만들어 뒤에 붙인다.
 */
export function applyPullRequestToGroups(
  groups: PartyPrGroup[],
  pullRequest: PartyPullRequest,
): PartyPrGroup[] {
  const owning = groups.findIndex((group) =>
    group.pullRequests.some((pr) => pr.id === pullRequest.id),
  );
  if (owning >= 0) {
    return groups.map((group, index) =>
      index === owning
        ? {
            ...group,
            pullRequests: group.pullRequests.map((pr) =>
              pr.id === pullRequest.id ? pullRequest : pr,
            ),
          }
        : group,
    );
  }

  const target = groups.findIndex((group) => belongsTo(group, pullRequest));
  if (target >= 0) {
    return groups.map((group, index) =>
      index === target ? { ...group, pullRequests: [pullRequest, ...group.pullRequests] } : group,
    );
  }

  return [
    ...groups,
    {
      githubUserId: pullRequest.authorGithubUserId ?? undefined,
      githubLogin: pullRequest.authorLogin || undefined,
      owner: false,
      pullRequests: [pullRequest],
    },
  ];
}

/** 묶음 이름 — 크루온 회원이면 이름, 외부면 GitHub 로그인명 */
export function groupLabel(group: PartyPrGroup): string {
  return group.memberName ?? group.githubLogin ?? '작성자 미상';
}
