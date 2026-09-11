import type { HostApproval, ReportItem } from '@/lib/types';
import {
  MOCK_ADMIN_AWARDS,
  MOCK_ADMIN_BADGES,
  MOCK_ADMIN_HOSTS,
  MOCK_ADMIN_KPIS,
  MOCK_ADMIN_REPORTS,
  MOCK_ADMIN_SIGNUP_CHART,
  MOCK_ADMIN_SIGNUP_TABLE,
} from '@/lib/mock';
import { API_BASE_URL, ApiError, http, mockResponse } from './client';

/**
 * 백엔드에 대응 엔드포인트가 아직 없는 화면이 쓰는 데모 데이터 플래그.
 *
 * 없는 경로로 요청하면 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다.
 *
 * 이 플래그를 쓰는 함수(통계·사업자·뱃지·신고·수상이력)는 대응하는 탭이 숨겨져 있어
 * 지금은 호출되지 않는다. 서버가 생기면 그 함수만 아래 파티·회원·증빙처럼 http 호출로 바꾸면 된다.
 */
const USE_MOCK: boolean = true;

/* ------------------------------------------------------------------ *
 * 공통
 * ------------------------------------------------------------------ */

/**
 * 관리자 목록 공통 형태.
 *
 * 서버가 두 가지 모양을 섞어 내려준다 - 증빙은 PageDto(`page`), 파티·회원은 Page<T> 그대로(`number`).
 * 화면이 그 차이를 알 필요가 없어 여기서 하나로 맞춘다. page 는 서버와 같은 0-based 다.
 */
export interface AdminPage<T> {
  items: T[];
  page: number;
  totalPages: number;
  totalElements: number;
}

interface RawPage<T> {
  content?: T[];
  /** PageDto */
  page?: number;
  /** Page<T> */
  number?: number;
  totalPages?: number;
  totalElements?: number;
}

function toAdminPage<T>(raw: RawPage<T>): AdminPage<T> {
  return {
    items: raw.content ?? [],
    page: raw.page ?? raw.number ?? 0,
    totalPages: raw.totalPages ?? 1,
    totalElements: raw.totalElements ?? 0,
  };
}

/** 관리자 목록 기본 페이지 크기 */
export const ADMIN_PAGE_SIZE = 20;

/* ------------------------------------------------------------------ *
 * 파티 — GET /adm/parties
 * ------------------------------------------------------------------ */

export interface AdminPartyRow {
  id: number;
  ownerName: string;
  partyName: string;
  title: string;
  topicType: string;
  status: string;
  partyTag: string;
  deadline: string | null;
  dDay: number;
  likeCount: number;
  viewCount: number;
  applicantCount: number | null;
  hidden: boolean;
}

/** hidden 을 생략하면 숨김 여부와 무관하게 전부 본다 */
export async function fetchAdminParties(
  keyword = '',
  page = 0,
  hidden?: boolean,
  size = ADMIN_PAGE_SIZE,
): Promise<AdminPage<AdminPartyRow>> {
  const raw = await http.get<RawPage<AdminPartyRow>>('/adm/parties', {
    query: { keyword, page, size, hidden },
  });
  return toAdminPage(raw);
}

/** PATCH /adm/parties/{partyId}/hidden — 전시·목록에서 감추거나 되돌린다 */
export async function updatePartyHidden(partyId: number, hidden: boolean): Promise<void> {
  await http.patch<void>(`/adm/parties/${partyId}/hidden`, { hidden });
}

/**
 * DELETE /adm/parties/{partyId} — 강제 삭제.
 *
 * 되돌릴 수 없다. 파티와 함께 지원 기록·좋아요·북마크까지 정리된다.
 * 이미 없는 파티에도 성공을 돌려주므로(멱등) 중복 호출은 안전하다.
 */
export async function deleteAdminParty(partyId: number): Promise<void> {
  await http.delete<void>(`/adm/parties/${partyId}`);
}

/* ------------------------------------------------------------------ *
 * 회원 — GET /adm/members
 * ------------------------------------------------------------------ */

export interface AdminMemberRow {
  id: number;
  email: string;
  name: string;
  nickname: string | null;
  role: 'MEMBER' | 'HOST' | 'ADMIN';
  active: boolean;
  createDate: string;
}

export async function fetchAdminMembers(
  keyword = '',
  page = 0,
  size = ADMIN_PAGE_SIZE,
): Promise<AdminPage<AdminMemberRow>> {
  const raw = await http.get<RawPage<AdminMemberRow>>('/adm/members', {
    query: { keyword, page, size },
  });
  return toAdminPage(raw);
}

/**
 * PATCH /adm/members/{memberId}/status — 정지 · 정지 해제.
 *
 * reason 은 정지할 때만 남는다(해제는 사유가 없다). 관리자 계정은 정지할 수 없어 409-2 가 온다.
 */
export async function updateMemberStatus(
  memberId: number,
  active: boolean,
  reason?: string,
): Promise<void> {
  await http.patch<void>(`/adm/members/${memberId}/status`, { active, reason });
}

export interface AdminMemberHistory {
  partyHistory: {
    ownedParties: {
      partyId: number;
      partyName: string;
      title: string;
      status: string;
      hidden: boolean;
      createDate: string;
    }[];
    appliedParties: {
      partyId: number;
      partyName: string;
      title: string;
      position: string;
      applicationStatus: string;
      message: string | null;
      appliedAt: string;
    }[];
  };
  achievements: {
    id: number;
    type: string;
    title: string;
    status: string;
    source: string;
    createDate: string;
  }[];
}

/** GET /adm/members/{memberId}/history — 만든 파티 · 지원한 파티 · 성취 */
export async function fetchMemberHistory(memberId: number): Promise<AdminMemberHistory> {
  return http.get<AdminMemberHistory>(`/adm/members/${memberId}/history`);
}

/* ------------------------------------------------------------------ *
 * 성취 증빙 검수 — /adm/goals/evidences
 * ------------------------------------------------------------------ */

export type EvidenceStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AdminEvidenceRow {
  goalId: number;
  ownerId: number;
  ownerName: string;
  /** 대회명 */
  title: string;
  result: string | null;
  awardDate: string | null;
  /** 외부 대회 공고·결과 페이지. 증빙과 대조할 근거가 된다 */
  contestUrl: string | null;
  fileName: string | null;
  mimeType: string | null;
  size: number | null;
  status: EvidenceStatus;
  reviewNote: string | null;
  /** 업로드 또는 검수 시점 */
  modifyDate: string;
}

export async function fetchAdminEvidences(
  status: EvidenceStatus = 'PENDING',
  page = 0,
  size = ADMIN_PAGE_SIZE,
): Promise<AdminPage<AdminEvidenceRow>> {
  const raw = await http.get<RawPage<AdminEvidenceRow>>('/adm/goals/evidences', {
    query: { status, page, size },
  });
  return toAdminPage(raw);
}

/** PATCH /adm/goals/{goalId}/evidence — 반려는 사유가 필수다(없으면 서버가 400-1) */
export async function reviewEvidence(
  goalId: number,
  status: 'APPROVED' | 'REJECTED',
  note?: string,
): Promise<void> {
  await http.patch<void>(`/adm/goals/${goalId}/evidence`, { status, note });
}

/**
 * GET /adm/goals/{goalId}/evidence — 증빙 파일 내려받기.
 *
 * 이 응답만 RsData 봉투가 아니라 파일 바이트다. http 헬퍼는 봉투를 벗겨 JSON 으로 읽으므로 쓸 수 없다.
 * 인증 쿠키가 필요해서 링크(`<a href>`)로도 못 열고, blob 으로 받아 저장을 띄운다.
 */
export async function downloadEvidence(goalId: number, fileName?: string | null): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/adm/goals/${goalId}/evidence`, {
    credentials: 'include',
  });

  if (!response.ok) {
    // 실패 응답은 RsData 봉투로 온다 - msg 를 그대로 쓴다
    const body = await response.json().catch(() => null);
    throw new ApiError(
      body?.msg ?? '증빙 파일을 내려받지 못했어요.',
      response.status,
      body,
      body?.resultCode,
    );
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName || `evidence-${goalId}`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  // 즉시 해제하면 저장이 시작되기 전에 blob 이 사라지는 브라우저가 있다
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/* ------------------------------------------------------------------ *
 * 아직 서버가 없는 화면 (탭 숨김 상태)
 * ------------------------------------------------------------------ */

/** GET /admin/stats?period= */
export async function fetchAdminStats(period: '주간' | '월간' | '연간' = '월간') {
  if (USE_MOCK) {
    return mockResponse({
      period,
      kpis: MOCK_ADMIN_KPIS,
      chart: MOCK_ADMIN_SIGNUP_CHART,
      table: MOCK_ADMIN_SIGNUP_TABLE,
    });
  }
  return http.get('/admin/stats', { query: { period } });
}

/** GET /admin/hosts — 사업자 가입 승인 대기 목록 */
export async function fetchAdminHosts(keyword = ''): Promise<HostApproval[]> {
  if (USE_MOCK) {
    return mockResponse(
      MOCK_ADMIN_HOSTS.filter(
        (row) => !keyword || row.company.includes(keyword) || row.bizNumber.includes(keyword),
      ),
    );
  }
  return http.get<HostApproval[]>('/admin/hosts', { query: { keyword } });
}

/** PATCH /admin/hosts/{id} */
export async function decideHostApproval(id: string, status: '승인' | '반려'): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/admin/hosts/${id}`, { status });
}

/** GET /admin/badges */
export async function fetchAdminBadges() {
  if (USE_MOCK) return mockResponse(MOCK_ADMIN_BADGES);
  return http.get('/admin/badges');
}

/** GET /admin/reports */
export async function fetchAdminReports(keyword = ''): Promise<ReportItem[]> {
  if (USE_MOCK) {
    return mockResponse(
      MOCK_ADMIN_REPORTS.filter(
        (row) => !keyword || row.target.includes(keyword) || row.reason.includes(keyword),
      ),
    );
  }
  return http.get<ReportItem[]>('/admin/reports', { query: { keyword } });
}

/** PATCH /admin/reports/{id} */
export async function decideReport(id: string, status: '처리완료' | '반려'): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/admin/reports/${id}`, { status });
}

/** GET /admin/awards */
export async function fetchAdminAwards(keyword = '') {
  if (USE_MOCK) {
    return mockResponse(
      MOCK_ADMIN_AWARDS.filter(
        (row) => !keyword || row.contest.includes(keyword) || row.team.includes(keyword),
      ),
    );
  }
  return http.get('/admin/awards', { query: { keyword } });
}
