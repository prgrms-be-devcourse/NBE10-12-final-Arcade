/**
 * 파티 GitHub 연동 · PR API — ApiV1PartyPrController 기준.
 *
 * 서버가 실제로 들고 있는 협업 기록은 **PR 뿐이다.** 커밋 목록은 없다(팀 스페이스의 커밋
 * 타임라인은 아직 데모다). GitHub App 을 파티 저장소에 설치하면 웹훅으로 PR 이 쌓이고,
 * 이 API 들이 그 상태와 목록을 돌려준다.
 */
import { MOCK_PARTY_GITHUB_CONNECTION, MOCK_PARTY_PULL_REQUESTS } from '@/lib/mock';
import type { PartyPrGroup } from './partyPrGroups';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

export { groupLabel, replacePrGroup, type PartyPrGroup } from './partyPrGroups';

/**
 * 백엔드 PartyGithubConnectionStatus.
 *
 * - PENDING               : 아직 연결을 시작하지 않음 (연결 행이 없을 때도 이 값이 온다)
 * - SYNCING               : 설치 직후 기존 PR 을 받아오는 중
 * - ACTIVE                : 연결됨. 웹훅으로 PR 이 들어온다
 * - INSTALLATION_REQUIRED : 설치가 지워졌거나 저장소가 설치 대상에서 빠졌다 — 다시 설치해야 한다
 * - APPROVAL_PENDING      : 조직 저장소라 관리자 승인 대기 중
 * - ERROR                 : 그 밖의 실패. lastError 에 사유가 온다
 */
export type PartyGithubStatus =
  | 'NOT_CONNECTED'
  | 'PENDING'
  | 'SYNCING'
  | 'ACTIVE'
  | 'DISCONNECTED'
  | 'ARCHIVED'
  | 'INSTALLATION_REQUIRED'
  | 'APPROVAL_PENDING'
  | 'ERROR';

/** 백엔드 PartyGithubConnectionDto */
export interface PartyGithubConnection {
  status: PartyGithubStatus;
  /** owner/repo 형태. 연결 전에는 비어 있다 */
  repositoryFullName: string | null;
  lastErrorCode: string | null;
  lastError: string | null;
}

/** 백엔드 PartyPrDto — 저장소에서 동기화된 PR 한 건 */
export interface PartyPullRequest {
  id: string;
  /** 저장소 안에서의 PR 번호 (#42) */
  number: number;
  title: string;
  htmlUrl: string;
  /** GitHub 원문 값 — 'open' | 'closed' */
  state: string;
  /** GitHub 로그인명 */
  authorLogin: string;
  /**
   * 작성자의 GitHub 계정 id. 크루온 회원의 연동 계정과 같으면 그 회원이 올린 PR 이다(ARC-108).
   * 웹훅이 작성자를 못 준 옛 기록은 없다.
   */
  authorGithubUserId?: number | null;
  draft: boolean;
  merged: boolean;
  baseBranch: string;
  headBranch: string;
  openedAt: string;
  closedAt: string | null;
  mergedAt: string | null;
  updatedAt: string;
}

/** 백엔드 GithubAppInstallUrlDto */
export interface GithubAppInstallUrl {
  installationUrl: string;
  state: string;
}

/** PATCH /api/v1/parties/{partyId}/github-repository — 파티장만 저장소 주소를 변경한다. */
export async function updatePartyGithubRepository(
  partyId: string,
  githubRepoUrl: string,
): Promise<string> {
  if (USE_MOCK) return mockResponse(githubRepoUrl);

  return http.patch<string>(`/parties/${partyId}/github-repository-url`, { githubRepoUrl });
}

interface PartyPrResponse extends Omit<PartyPullRequest, 'id' | 'updatedAt'> {
  id: number;
  githubUpdatedAt: string;
}

function toPullRequest(dto: PartyPrResponse): PartyPullRequest {
  const { id, githubUpdatedAt, ...rest } = dto;
  return { ...rest, id: String(id), updatedAt: githubUpdatedAt };
}

/**
 * 서버 경로에 넣을 수 있는 파티 id 인지 본다.
 *
 * 팀 스페이스 경로에는 아직 목 슬러그('paybridge')가 들어올 수 있는데, 그대로 요청하면
 * 400 이 난다. 숫자가 아니면 서버를 부르지 않는다.
 */
export function isServerPartyId(partyId: string): boolean {
  return /^\d+$/.test(partyId);
}

/** GET /api/v1/parties/{partyId}/github-connection — 연결 상태 */
export async function fetchPartyGithubConnection(partyId: string): Promise<PartyGithubConnection> {
  if (USE_MOCK) return mockResponse(MOCK_PARTY_GITHUB_CONNECTION);
  // 연결 상태는 사용자 권한과 직전 연결/해제 결과에 따라 달라진다. 서버 렌더에서
  // 이전 NOT_CONNECTED 응답을 재사용하면 새로고침 뒤에도 버튼이 사라진 것처럼 보인다.
  return http.get<PartyGithubConnection>(`/parties/${partyId}/github-connection`, { cache: 'no-store' });
}

/**
 * 상태를 못 읽으면 null 을 돌려주는 버전.
 *
 * 팀 스페이스는 서버 컴포넌트에서 읽는데, 비로그인(401)이나 없는 파티(404)에 그대로 던지면
 * 페이지 전체가 죽는다. 연결 카드만 비우는 편이 낫다.
 */
export async function fetchPartyGithubConnectionOrNull(
  partyId: string,
): Promise<PartyGithubConnection | null> {
  if (!USE_MOCK && !isServerPartyId(partyId)) return null;

  try {
    return await fetchPartyGithubConnection(partyId);
  } catch (error) {
    if (error instanceof ApiError) return null;
    throw error;
  }
}

/** 백엔드 PartyPrByMemberDto */
interface PartyPrGroupResponse {
  memberId: number | null;
  memberName: string | null;
  githubUserId: number | null;
  githubLogin: string | null;
  owner: boolean;
  pullRequests: PartyPrResponse[];
}

function toPrGroup(dto: PartyPrGroupResponse): PartyPrGroup {
  return {
    memberId: dto.memberId != null ? String(dto.memberId) : undefined,
    memberName: dto.memberName ?? undefined,
    githubUserId: dto.githubUserId ?? undefined,
    githubLogin: dto.githubLogin ?? undefined,
    owner: dto.owner,
    pullRequests: dto.pullRequests.map(toPullRequest),
  };
}

/** 데모 데이터에는 회원 연결이 없어 작성자별 외부 묶음으로만 나눈다 */
function mockPrGroups(): PartyPrGroup[] {
  const byAuthor = new Map<string, PartyPullRequest[]>();
  MOCK_PARTY_PULL_REQUESTS.forEach((pullRequest) => {
    byAuthor.set(pullRequest.authorLogin, [
      ...(byAuthor.get(pullRequest.authorLogin) ?? []),
      pullRequest,
    ]);
  });
  return Array.from(byAuthor, ([githubLogin, pullRequests]) => ({
    githubLogin,
    owner: false,
    pullRequests,
  }));
}

/**
 * GET /api/v1/parties/{partyId}/pull-requests/grouped-by-member — 담당자별 PR 묶음 (ARC-108).
 *
 * **파티장과 승인된 파티원이 PR 0건이어도 모두 들어온다.** 파티원 목록 API 가 따로 없어
 * 팀 스페이스의 팀원 목록도 이 응답으로 그린다.
 */
export async function fetchPartyPrGroups(partyId: string): Promise<PartyPrGroup[]> {
  if (USE_MOCK) return mockResponse(mockPrGroups());

  const result = await http.get<PartyPrGroupResponse[]>(
    `/parties/${partyId}/pull-requests/grouped-by-member`,
    { cache: 'no-store' },
  );
  return result.map(toPrGroup);
}

/** 못 읽으면 빈 목록을 돌려주는 버전 (서버 컴포넌트용) */
export async function fetchPartyPrGroupsOrEmpty(partyId: string): Promise<PartyPrGroup[]> {
  if (!USE_MOCK && !isServerPartyId(partyId)) return [];

  try {
    return await fetchPartyPrGroups(partyId);
  } catch (error) {
    if (error instanceof ApiError) return [];
    throw error;
  }
}

/** 담당자별 SSE 의 snapshot 이벤트. 같은 매퍼를 써서 목록 조회와 어긋나지 않게 한다 */
export function parsePartyPrGroups(json: string): PartyPrGroup[] {
  return (JSON.parse(json) as PartyPrGroupResponse[]).map(toPrGroup);
}

/** 담당자별 SSE 의 pull-request-group 이벤트 — 바뀐 작성자 묶음 하나가 통째로 온다 */
export function parsePartyPrGroup(json: string): PartyPrGroup {
  return toPrGroup(JSON.parse(json) as PartyPrGroupResponse);
}

/** 팀원 목록용 — 크루온 회원인 묶음만, 파티장을 앞에 둔다 */
export function membersOf(groups: PartyPrGroup[]): PartyPrGroup[] {
  return groups.filter((group) => group.memberId);
}

/**
 * POST /api/v1/parties/{partyId}/github-app/install — GitHub App 설치 URL 발급.
 *
 * 파티장만 부를 수 있고(403), 파티에 GitHub 저장소 주소가 없으면 400-22 로 거절된다.
 * 받은 주소로 이동하면 GitHub 설치 화면이 열리고, 설치가 끝나면 서버의 setup 콜백이
 * redirectUrl(기본값: 파티 상세)로 되돌려 보낸다.
 */
export async function startPartyGithubInstall(
  partyId: string,
  redirectUrl?: string,
): Promise<GithubAppInstallUrl> {
  if (USE_MOCK) {
    return mockResponse({ installationUrl: 'https://github.com/apps/crewon/installations/new', state: 'mock-state' });
  }

  return http.post<GithubAppInstallUrl>(`/parties/${partyId}/github-app/install`, undefined, {
    query: { redirectUrl },
  });
}

/** Party와 무관하게 GitHub App 설치 또는 설치 범위 추가를 시작한다. */
export async function startGithubAppInstall(): Promise<GithubAppInstallUrl> {
  if (USE_MOCK) return mockResponse({ installationUrl: 'https://github.com/apps/crewon/installations/new', state: '' });
  return http.post<GithubAppInstallUrl>('/github-app/install');
}

/** GitHub App 사용자 권한 URL 발급. 레포 목록을 보기 전에 필요하다. */
export async function startGithubAppUserAuthorization(): Promise<{ authorizationUrl: string }> {
  if (USE_MOCK) return mockResponse({ authorizationUrl: 'https://github.com/login/oauth/authorize' });
  return http.post<{ authorizationUrl: string }>('/github-app/user/authorize');
}

export interface ConnectableGithubRepository {
  installationRepositoryId: number;
  fullName: string;
  htmlUrl: string;
}

/** GitHub App 설치 범위와 현재 사용자의 권한을 모두 만족하는 레포 목록. */
export async function fetchConnectableGithubRepositories(): Promise<ConnectableGithubRepository[]> {
  if (USE_MOCK) return mockResponse([]);
  return http.get<ConnectableGithubRepository[]>('/github-app/repositories');
}

/** 선택한 installation 레포를 Party에 연결한다. */
export async function connectPartyGithubRepository(partyId: string, installationRepositoryId: number): Promise<void> {
  if (USE_MOCK) return;
  await http.post(`/parties/${partyId}/github-repository`, { installationRepositoryId });
}

/** Party와 레포의 연결만 해제한다. GitHub App 설치 및 PR 이력은 유지된다. */
export async function disconnectPartyGithubRepository(partyId: string): Promise<void> {
  if (USE_MOCK) return;
  await http.delete(`/parties/${partyId}/github-repository`);
}

export interface GithubAppUserAuthorizationStatus {
  githubSocialLinked: boolean;
  authorized: boolean;
  expired: boolean;
  accessTokenExpiresAt: string | null;
}

export async function fetchGithubAppUserAuthorizationStatus(): Promise<GithubAppUserAuthorizationStatus> {
  if (USE_MOCK) return mockResponse({ githubSocialLinked: true, authorized: true, expired: false, accessTokenExpiresAt: null });
  return http.get<GithubAppUserAuthorizationStatus>('/github-app/user/authorization-status');
}

/** '내 PR만 보기'에 사용하는 현재 회원의 GitHub PR 전체 목록. */
export async function fetchMyGithubPullRequests(): Promise<PartyPullRequest[]> {
  if (USE_MOCK) return mockResponse(MOCK_PARTY_PULL_REQUESTS);
  const result = await http.get<PartyPrResponse[]>('/pull-requests/me');
  return result.map(toPullRequest);
}
