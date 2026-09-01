/**
 * 성취(Goal) API — ApiV1GoalController 기준.
 *
 * 백엔드에 실제로 구현된 엔드포인트는 아직 등록(POST) 하나뿐이다.
 * 조회·수정·삭제(GET /goals/me, GET /goals/{id}, PATCH, DELETE)가 붙으면 여기에 이어서 추가한다.
 */
import type { Achievement, GoalStatus, GoalType, ID } from '@/lib/types';
import { USE_MOCK, http, mockResponse } from './client';

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
