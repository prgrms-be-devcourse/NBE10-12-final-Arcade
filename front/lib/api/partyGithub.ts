/**
 * 파티 GitHub 연동 · PR API — ApiV1PartyPrController 기준.
 *
 * 서버가 실제로 들고 있는 협업 기록은 **PR 뿐이다.** 커밋 목록은 없다(팀 스페이스의 커밋
 * 타임라인은 아직 데모다). GitHub App 을 파티 저장소에 설치하면 웹훅으로 PR 이 쌓이고,
 * 이 API 들이 그 상태와 목록을 돌려준다.
 */
import { MOCK_PARTY_GITHUB_CONNECTION, MOCK_PARTY_PULL_REQUESTS } from '@/lib/mock';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

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
  | 'PENDING'
  | 'SYNCING'
  | 'ACTIVE'
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
  /** GitHub 로그인명. 아직 크루온 회원과 연결되지 않는다 */
  authorLogin: string;
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
function isServerPartyId(partyId: string): boolean {
  return /^\d+$/.test(partyId);
}

/** GET /api/v1/parties/{partyId}/github-connection — 연결 상태 */
export async function fetchPartyGithubConnection(partyId: string): Promise<PartyGithubConnection> {
  if (USE_MOCK) return mockResponse(MOCK_PARTY_GITHUB_CONNECTION);
  return http.get<PartyGithubConnection>(`/parties/${partyId}/github-connection`);
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

/**
 * GET /api/v1/parties/{partyId}/pull-requests — 동기화된 PR 목록.
 * 최신 갱신순으로 온다. 연결 전이면 빈 배열이다.
 */
export async function fetchPartyPullRequests(partyId: string): Promise<PartyPullRequest[]> {
  if (USE_MOCK) return mockResponse(MOCK_PARTY_PULL_REQUESTS);

  const result = await http.get<PartyPrResponse[]>(`/parties/${partyId}/pull-requests`);
  return result.map(toPullRequest);
}

/** 목록을 못 읽으면 빈 배열을 돌려주는 버전 (서버 컴포넌트용) */
export async function fetchPartyPullRequestsOrEmpty(partyId: string): Promise<PartyPullRequest[]> {
  if (!USE_MOCK && !isServerPartyId(partyId)) return [];

  try {
    return await fetchPartyPullRequests(partyId);
  } catch (error) {
    if (error instanceof ApiError) return [];
    throw error;
  }
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
