/**
 * 담당자별 PR 묶음의 순수 로직.
 *
 * 서버(ARC-130)는 grouped 스트림에 **바뀐 작성자 묶음 하나를 통째로** 보낸다(`pull-request-group`).
 * 그래서 화면이 할 일은 같은 작성자의 묶음을 찾아 갈아 끼우는 것뿐이다 —
 * PR 한 건을 받아 어느 묶음에 넣을지 화면이 판단하던 예전 방식은 서버가 걷어갔다.
 *
 * 의존성이 없어 partyPrGroups.check.ts 로 바로 돌려볼 수 있다.
 */
import type { PartyPullRequest } from './partyGithub';

/**
 * 백엔드 PartyPrByMemberDto — 파티 PR 을 GitHub 계정 단위로 묶은 한 덩어리.
 *
 * memberId 가 있으면 크루온 회원(파티장·승인 파티원)이고, 없으면 파티원과 연결되지 않은
 * 외부 GitHub 작성자다. 회원 묶음은 PR 이 0건이어도 내려온다 — 그래서 팀원 목록으로도 쓴다.
 */
export interface PartyPrGroup {
  memberId?: string;
  memberName?: string;
  /** 연동된 GitHub 계정. 회원이 연동하지 않았거나 작성자를 모르는 묶음이면 없다 */
  githubUserId?: number;
  /** 작성자의 GitHub 로그인명 */
  githubLogin?: string;
  owner: boolean;
  pullRequests: PartyPullRequest[];
}

/**
 * 같은 작성자의 묶음인지.
 *
 * 작성자를 모르는 묶음(githubUserId 없음)은 **GitHub 미연동 회원** 묶음과 헷갈리기 쉽다 —
 * 둘 다 githubUserId 가 비어 있어서다. 회원이 아닌 쪽('작성자 미상')하고만 짝지운다.
 */
function isSameAuthor(group: PartyPrGroup, incoming: PartyPrGroup): boolean {
  if (incoming.githubUserId == null) {
    return group.memberId === undefined && group.githubUserId === undefined;
  }
  return group.githubUserId === incoming.githubUserId;
}

/**
 * SSE 로 들어온 작성자 묶음을 목록에 반영한다.
 *
 * 있던 작성자면 자리를 지킨 채 내용만 바꾸고(파티원 순서가 튀지 않게),
 * 처음 보는 외부 작성자면 뒤에 붙인다.
 */
export function replacePrGroup(
  groups: PartyPrGroup[],
  incoming: PartyPrGroup,
): PartyPrGroup[] {
  const index = groups.findIndex((group) => isSameAuthor(group, incoming));
  if (index < 0) return [...groups, incoming];

  return groups.map((group, i) =>
    i === index
      ? // 회원 정보는 목록 조회 쪽이 더 정확할 수 있어(스냅샷 기준) 이름·파티장 여부는 지킨다
        { ...group, githubLogin: incoming.githubLogin ?? group.githubLogin, pullRequests: incoming.pullRequests }
      : group,
  );
}

/** 묶음 이름 — 크루온 회원이면 이름, 외부면 GitHub 로그인명 */
export function groupLabel(group: PartyPrGroup): string {
  return group.memberName ?? group.githubLogin ?? '작성자 미상';
}
