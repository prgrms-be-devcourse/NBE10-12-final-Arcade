/**
 * 성취(Goal) API — ApiV1GoalController 기준.
 *
 * 등록(POST /goals) · 내 목록(GET /goals/me) · 상세 조회(GET /goals/{goalId})가 붙어 있다.
 * 수정·삭제(PATCH, DELETE)는 화면 흐름만 먼저 맞춰 둔 상태다.
 */
import type { Achievement, GoalSource, GoalStatus, GoalType, ID, PartyStatus } from '@/lib/types';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES, mockGoalDetail } from '@/lib/mock';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

/**
 * 백엔드 PositionType.
 *
 * 화면 전역 PositionType 은 아직 BACK·FRONT 뿐이라 서버 enum 을 다 담지 못한다.
 * 파티 포지션 정리가 끝날 때까지 성취 응답에서만 쓰는 넓은 타입을 따로 둔다.
 */
export type GoalPositionType = 'BACK' | 'FRONT' | 'UIUX' | 'PM';

export const GOAL_POSITION_LABELS: Record<GoalPositionType, string> = {
  BACK: '백엔드',
  FRONT: '프론트엔드',
  UIUX: 'UI/UX',
  PM: '기획·PM',
};

/**
 * 증빙 파일 한 건의 메타데이터. 파일 자체는 Object Storage 에 있고 응답에는 이 값들만 온다.
 */
export interface EvidenceFileFields {
  storageKey?: string;
  fileName: string;
  mimeType?: string;
  size?: number;
}

/** 백엔드 GoalDetailDto — type 에 해당하지 않는 필드는 응답에서 빠진다(@JsonInclude NON_NULL) */
export interface GoalDetailFields {
  /** PROJECT(전시 게시 후) · CHECKLIST */
  title?: string;
  /** PROJECT(전시 게시 후) · CONTEST(수상 결과) */
  result?: string;
  /** PROJECT */
  positionType?: GoalPositionType;
  startDate?: string;
  endDate?: string;
  /** CONTEST */
  contestName?: string;
  isTeam?: boolean;
  awardDate?: string;
  /** 외부 대회 원본 페이지 주소. [서버 미지원] PersonalContest 에 아직 컬럼이 없다 */
  contestUrl?: string;
  targetContestId?: number;
  /**
   * CONTEST — 증빙 파일 한 건. 서버가 PersonalContest 에 컬럼 4개로 들고 있던 시절의 모양이다.
   * evidences 가 오면 그쪽이 전체 목록이라 이 값들은 보지 않는다.
   */
  evidenceStorageKey?: string;
  evidenceFileName?: string;
  evidenceMimeType?: string;
  evidenceSize?: number;
  /**
   * CONTEST — 증빙 파일 목록. 수상 확인서 + 결과 발표 화면처럼 여러 건을 남길 수 있다.
   * [서버 미지원] GOAL_EVIDENCE 분리 전까지는 오지 않는다
   * (docs/프론트-API연동_백엔드_수정요청.md ⑤-4).
   */
  evidences?: EvidenceFileFields[];
  /** CHECKLIST */
  memo?: string;
  targetDate?: string;
  /**
   * 연결된 개인 TODO의 id. 항목까지 보려면 상세 응답의 todo 블록을 쓴다.
   * [서버 미지원] PersonalChecklist 에 FK 는 있지만 응답에 아직 담기지 않는다.
   */
  todoId?: number | null;
}

/** 백엔드 GoalDto */
export interface GoalResponse {
  id: number;
  ownerId: number;
  type: GoalType;
  status: GoalStatus;
  source: 'SELF_REPORTED' | 'PLATFORM_VERIFIED';
  sourcePartyId: number | null;
  partyAssembleToMemberId: number | null;
  detail: GoalDetailFields;
  createDate: string;
  modifyDate: string;
}

/** 백엔드 PartyPrDto — 파티 저장소에서 동기화된 PR 한 건 */
export interface PartyPrResponse {
  id: number;
  githubPrId: number;
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
  githubUpdatedAt: string;
}

/**
 * 백엔드 ProjectContextDto — PROJECT 성취가 가리키는 파티 정보.
 *
 * PROJECT 성취는 "내가 이 파티에 참여했다"는 연결만 갖고 내용은 파티에 있다.
 * 그래서 상세 화면은 이 블록으로 구성한다.
 */
export interface ProjectContextResponse {
  partyId: number;
  partyName: string;
  title: string;
  partyStatus: PartyStatus;
  deadline: string;
  /** 등록된 저장소. 없으면 응답에서 빠진다 */
  githubRepoUrl?: string;
  /** 이 파티에서 맡은 포지션. 파티장은 지원 절차가 없어 값이 없다 */
  myPositionType?: GoalPositionType;
  partyOwner: boolean;
  /** 본인 성취를 볼 때만 채워진다. 남의 성취에서는 빈 배열 */
  pullRequests: PartyPrResponse[];
}

/** 연결된 개인 TODO의 할 일 한 건 (백엔드 PersonalTodoItemDto) */
export interface TodoItemResponse {
  id: number;
  content: string;
  /** 성취 상세에는 완료한 항목만 오므로 항상 true 다 */
  done: boolean;
  /** 체크한 시각. 이 값으로 정렬돼 온다 */
  doneAt: string | null;
  sortOrder: number;
}

/**
 * CHECKLIST 성취에 연결된 개인 TODO (백엔드 TodoContextDto).
 *
 * 개인 TODO 자체는 비공개지만, 성취에 연결한 것을 공개 의사표시로 보고 남의 성취에서도 보여준다.
 * [서버 미지원] PersonalChecklist 에 FK 는 있지만 상세 응답에 이 블록이 아직 없다.
 */
export interface TodoContextResponse {
  todoId: number;
  title: string;
  category: string;
  memo?: string;
  status: GoalStatus;
  /**
   * 완료한 항목만, 해낸 순서대로 온다. 미완료 항목과 진행률은 오지 않는다 -
   * 해낸 것은 발자취지만 아직 못 한 일은 소유자 개인의 할 일이다.
   */
  items: TodoItemResponse[];
}

/** 백엔드 GoalDetailResponseDto — GET /goals/{goalId} */
export interface GoalDetailResponse {
  id: number;
  ownerId: number;
  ownerName: string;
  type: GoalType;
  status: GoalStatus;
  source: GoalSource;
  sourcePartyId: number | null;
  detail: GoalDetailFields;
  /** type 이 PROJECT 일 때만 온다 */
  project?: ProjectContextResponse;
  /** CHECKLIST 성취에 개인 TODO 가 연결됐을 때만 온다 */
  todo?: TodoContextResponse;
  createDate: string;
  modifyDate: string;
}

/**
 * 자기신고 등록 요청 (GoalCreateReqBody).
 *
 * type 에 따라 detail 의 필수 필드가 다르다.
 * - CONTEST   : contestName 필수
 * - CHECKLIST : title 필수
 * - PROJECT   : 파티 확정 시 자동 생성되는 전용 타입이라 등록할 수 없다 (400-4)
 */
/**
 * 타입별 세부 정보 (백엔드 GoalDetailReqBody).
 * type 에 해당하지 않는 필드는 서버가 무시한다. null 은 값을 비우겠다는 뜻이다.
 */
/** 증빙 파일 한 건. 파일 자체는 업로드 API 가 생기면 따로 올라가고 요청엔 메타데이터만 담는다 */
export interface EvidenceFilePayload {
  fileName: string;
  mimeType?: string | null;
  size?: number | null;
}

export interface GoalDetailPayload {
  /** CONTEST */
  contestName?: string;
  isTeam?: boolean;
  result?: string;
  /** yyyy-MM-dd */
  awardDate?: string;
  /**
   * 외부 대회 원본 페이지 주소. 비울 땐 null.
   * 자기신고 대회 성취는 크루온에 없는 외부 대회를 기록하는 것이라 링크를 직접 받는다.
   * [서버 미지원] PersonalContest 에 컬럼이, GoalDetailReqBody 에 필드가 아직 없다.
   */
  contestUrl?: string | null;
  /**
   * 증빙 파일 목록 전체. 보낸 목록이 곧 저장될 목록이다 — 빈 배열은 다 지우겠다는 뜻이다.
   * [서버 미지원] GOAL_EVIDENCE 분리 전까지 서버가 무시하므로 첫 파일은 아래 3개 필드로도 함께 보낸다 (⑤-4).
   */
  evidences?: EvidenceFilePayload[];
  /**
   * 증빙 파일 한 건의 메타데이터. 파일 자체는 Object Storage 로 따로 올라간다.
   * 서버가 아직 한 건만 보관해서 남겨둔 자리이고, evidences 를 받게 되면 걷어낸다.
   */
  evidenceFileName?: string | null;
  evidenceMimeType?: string | null;
  evidenceSize?: number | null;

  /** CHECKLIST */
  title?: string;
  memo?: string;
  /** yyyy-MM-dd */
  targetDate?: string;
  /**
   * 연결할 개인 TODO. 그 TODO의 할 일 목록이 성취의 진행 과정으로 붙는다. 끊을 땐 null.
   * [서버 미지원] PersonalChecklist 에 FK 는 있지만 GoalDetailReqBody 에 필드가 아직 없다.
   */
  todoId?: number | null;
}

export interface CreateGoalPayload {
  type: Extract<GoalType, 'CONTEST' | 'CHECKLIST'>;
  status: GoalStatus;
  detail: GoalDetailPayload;
}

/**
 * 상세 응답에서 증빙 파일 목록을 뽑는다.
 *
 * 서버가 GOAL_EVIDENCE 로 나뉘면 detail.evidences 로 오고, 그 전까지는 한 건 분량의
 * evidence* 필드만 온다. 화면은 어느 쪽이든 목록 하나로 다룬다 (⑤-4).
 */
export function evidenceFilesOf(detail: GoalDetailFields): EvidenceFileFields[] {
  if (detail.evidences?.length) return detail.evidences;

  const fileName = detail.evidenceFileName ?? detail.evidenceStorageKey;
  if (!fileName) return [];

  return [
    {
      storageKey: detail.evidenceStorageKey,
      fileName,
      mimeType: detail.evidenceMimeType,
      size: detail.evidenceSize,
    },
  ];
}

/** 파일 크기 표기. 서버는 바이트로 주고받는다. 딱 떨어지면 소수점을 붙이지 않는다(10.0MB → 10MB) */
export function formatFileSize(size?: number): string {
  if (size == null) return '';
  if (size < 1024) return `${size}B`;
  const round = (value: number) => value.toFixed(1).replace(/\.0$/, '');
  if (size < 1024 * 1024) return `${round(size / 1024)}KB`;
  return `${round(size / 1024 / 1024)}MB`;
}

/** yyyy-MM-dd 또는 ISO 문자열에서 연도만 뽑는다 */
function yearOf(date?: string): string {
  return date ? date.slice(0, 4) : '';
}

/** 화면에 쓰는 기간 문구. 백엔드에 period 필드가 없어 날짜로 조립한다 */
function periodOf(goal: GoalResponse): string {
  const { detail, status } = goal;

  if (goal.type === 'PROJECT') {
    const start = detail.startDate?.replace(/-/g, '.').slice(0, 7) ?? '';
    if (detail.endDate) return `${start} ~ ${detail.endDate.replace(/-/g, '.').slice(0, 7)}`;
    return start ? `${start} ~ 진행중` : '';
  }

  if (goal.type === 'CONTEST') {
    return detail.awardDate?.replace(/-/g, '.').slice(0, 7) ?? '';
  }

  const target = detail.targetDate?.replace(/-/g, '.').slice(0, 7) ?? '';
  if (!target) return '';
  if (status === 'ACHIEVED') return target;
  if (status === 'WANT') return `${target} 예정`;
  return `${target} ~ 진행중`;
}

/**
 * 백엔드 GoalDto 를 화면이 쓰는 Achievement 로 옮긴다.
 *
 * 백엔드에 아직 없는 필드는 비워 둔다.
 * - tags  : 별도 테이블이 필요해 미구현
 * - links : 미구현
 * - PROJECT 의 title : 전시 게시(PARTY_SHOWCASE) 전까지 null 이라 파티명을 대신 보여줄지 미정
 */
export function toAchievement(goal: GoalResponse): Achievement {
  const dateForYear =
    goal.detail.startDate ?? goal.detail.awardDate ?? goal.detail.targetDate ?? goal.createDate;

  return {
    id: String(goal.id) as ID,
    type: goal.type,
    status: goal.status,
    source: goal.source,
    year: yearOf(dateForYear),
    period: periodOf(goal),
    title: goal.detail.title ?? goal.detail.contestName ?? '',
    description: goal.detail.result ?? goal.detail.memo ?? '',
    tags: [],
    links: [],
    sourcePartyId: goal.sourcePartyId != null ? (String(goal.sourcePartyId) as ID) : undefined,
  };
}

/** POST /api/v1/goals — 성취 자기신고 등록 */
export async function createGoal(payload: CreateGoalPayload): Promise<Achievement> {
  if (USE_MOCK) {
    return mockResponse<Achievement>({
      id: `achv-${Date.now()}`,
      type: payload.type,
      status: payload.status,
      source: 'SELF_REPORTED',
      year: yearOf(payload.detail.awardDate ?? payload.detail.targetDate),
      period: '',
      title: payload.detail.title ?? payload.detail.contestName ?? '',
      description: payload.detail.result ?? payload.detail.memo ?? '',
      tags: [],
      links: [],
    });
  }

  const created = await http.post<GoalResponse>('/goals', payload);
  return toAchievement(created);
}

/**
 * 목록 응답의 과도기 형태.
 *
 * 검색 개편에서 페이지네이션을 걷어내 목록이 배열로 오지만, 그 전 서버는 스프링 Page 를
 * 그대로 내려준다(성취는 content 안에 있다). 개편이 머지되기 전까지 두 모양을 다 받는다.
 */
interface GoalPageResponse {
  content: GoalResponse[];
}

export interface MyGoalsQuery {
  status?: GoalStatus;
  type?: GoalType;
  source?: GoalSource;
}

/**
 * GET /api/v1/goals/me — 내 성취 목록 (최신순).
 *
 * 본인 것만 돌려주므로 소유자 조건을 따로 넘기지 않는다.
 * status·type·source 필터는 모두 선택이고, 넘기지 않으면 조건 없이 전부 온다.
 *
 * 페이지 파라미터는 보내지 않는다 — 서버가 목록 전체를 한 번에 돌려준다.
 */
export async function fetchMyGoals(query: MyGoalsQuery = {}): Promise<Achievement[]> {
  if (USE_MOCK) return mockResponse(MOCK_PROFILES[MOCK_CURRENT_USER_ID]?.achievements ?? []);

  const result = await http.get<GoalPageResponse | GoalResponse[]>('/goals/me', {
    query: {
      status: query.status,
      type: query.type,
      source: query.source,
    },
  });

  const goals = Array.isArray(result) ? result : result.content;
  return goals.map(toAchievement);
}

/**
 * 로그인하지 않았으면 빈 목록을 돌려주는 버전.
 *
 * 마이페이지는 서버 컴포넌트에서 프로필과 함께 읽는데, 여기서 401 을 그대로 던지면
 * 프로필이 있는데도 화면 전체가 500 으로 죽는다. 목록이 비는 편이 낫다.
 */
export async function fetchMyGoalsOrEmpty(query: MyGoalsQuery = {}): Promise<Achievement[]> {
  try {
    return await fetchMyGoals(query);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return [];
    throw error;
  }
}

/**
 * GET /api/v1/goals/{goalId} — 성취 상세 조회.
 *
 * 성취는 전체 공개라(기획서 2.5) 남의 성취도 볼 수 있지만, 서버 인가 규칙상 로그인은 필요하다.
 * 미로그인은 401, 없는 성취는 404 로 떨어지므로 호출하는 쪽에서 갈라 처리한다.
 */
export async function fetchGoalDetail(goalId: string | number): Promise<GoalDetailResponse> {
  if (USE_MOCK) return mockResponse(mockGoalDetail(String(goalId)));
  return http.get<GoalDetailResponse>(`/goals/${goalId}`);
}

/**
 * DELETE /api/v1/goals/{goalId} — 성취 삭제.
 *
 * 자기신고 성취만 지울 수 있다. 자동기록 성취는 서버가 409-1 로 막는다.
 * 서버 구현은 아직 붙지 않았고(작업표 8번), 화면 쪽 흐름만 먼저 맞춰 둔다.
 */
export async function deleteGoal(goalId: string | number): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.delete<void>(`/goals/${goalId}`);
}

/**
 * 성취 수정 요청.
 *
 * type 은 바꿀 수 없다 — 수상 기록을 체크리스트로 바꾸는 건 다른 성취를 만드는 일이다.
 * detail 은 등록(POST)과 같은 모양이고, 해당 타입에 없는 필드는 서버가 무시한다.
 */
export type UpdateGoalPayload = Pick<CreateGoalPayload, 'status' | 'detail'>;

/**
 * PATCH /api/v1/goals/{goalId} — 성취 수정.
 *
 * 자기신고 성취만 고칠 수 있다. 자동기록 성취는 서버가 409-1 로 막는다.
 * 서버 구현은 아직 붙지 않았고(작업표 7번), 화면 쪽 흐름만 먼저 맞춰 둔다.
 */
export async function updateGoal(
  goalId: string | number,
  payload: UpdateGoalPayload,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch<void>(`/goals/${goalId}`, payload);
}
