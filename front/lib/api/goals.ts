/**
 * 성취(Goal) API — ApiV1GoalController 기준.
 *
 * 붙어 있는 엔드포인트는 등록(POST /goals)과 내 목록(GET /goals/me) 두 개다.
 * 상세·수정·삭제(GET /goals/{id}, PATCH, DELETE)가 붙으면 여기에 이어서 추가한다.
 */
import type { Achievement, GoalSource, GoalStatus, GoalType, ID } from '@/lib/types';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES } from '@/lib/mock';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

/** 백엔드 GoalDetailDto — type 에 해당하지 않는 필드는 응답에서 빠진다(@JsonInclude NON_NULL) */
export interface GoalDetailResponse {
  /** PROJECT(전시 게시 후) · CHECKLIST */
  title?: string;
  /** PROJECT(전시 게시 후) · CONTEST(수상 결과) */
  result?: string;
  /** PROJECT */
  positionType?: 'BACK' | 'FRONT' | 'UIUX' | 'PM';
  startDate?: string;
  endDate?: string;
  /** CONTEST */
  contestName?: string;
  isTeam?: boolean;
  awardDate?: string;
  targetContestId?: number;
  /** CHECKLIST */
  memo?: string;
  targetDate?: string;
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
  viewCount: number;
  detail: GoalDetailResponse;
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
export interface CreateGoalPayload {
  type: Extract<GoalType, 'CONTEST' | 'CHECKLIST'>;
  status: GoalStatus;
  detail: {
    contestName?: string;
    isTeam?: boolean;
    result?: string;
    /** yyyy-MM-dd */
    awardDate?: string;
    title?: string;
    memo?: string;
    /** yyyy-MM-dd */
    targetDate?: string;
  };
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
    viewCount: goal.viewCount,
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
      viewCount: 0,
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
