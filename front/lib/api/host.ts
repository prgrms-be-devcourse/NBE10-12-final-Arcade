import type { HostCompany, HostContestSummary } from '@/lib/types';
import {
  MOCK_HOST_COMPANY,
  MOCK_HOST_CONTESTS,
  MOCK_HOST_FILES,
  MOCK_HOST_STATS,
  MOCK_HOST_TEAMS,
} from '@/lib/mock';
import { http, mockResponse } from './client';

/**
 * 백엔드에 대응 엔드포인트가 아직 없는 모듈이다.
 *
 * 없는 경로로 요청해도 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다.
 * 그래서 실제 API 가 생기기 전까지는 이 상수로 데모 데이터만 쓰도록 고정한다.
 *
 * 서버가 준비되면 이 상수를 지우고 client 의 USE_MOCK 을 다시 import 하면 아래 http 호출이 살아난다.
 */
const USE_MOCK: boolean = true;


/** GET /host/me */
export async function fetchHostCompany(): Promise<HostCompany> {
  if (USE_MOCK) return mockResponse(MOCK_HOST_COMPANY);
  return http.get<HostCompany>('/host/me');
}

/** PUT /host/me */
export async function updateHostCompany(payload: Partial<HostCompany>): Promise<HostCompany> {
  if (USE_MOCK) return mockResponse({ ...MOCK_HOST_COMPANY, ...payload });
  return http.put<HostCompany>('/host/me', payload);
}

/** GET /host/stats */
export async function fetchHostStats() {
  if (USE_MOCK) return mockResponse(MOCK_HOST_STATS);
  return http.get('/host/stats');
}

/** GET /host/contests */
export async function fetchHostContests(): Promise<HostContestSummary[]> {
  if (USE_MOCK) return mockResponse(MOCK_HOST_CONTESTS);
  return http.get<HostContestSummary[]>('/host/contests');
}

/** GET /host/teams */
export async function fetchHostTeams() {
  if (USE_MOCK) return mockResponse(MOCK_HOST_TEAMS);
  return http.get('/host/teams');
}

/** GET /host/files — 사업자 증빙 서류 */
export async function fetchHostFiles() {
  if (USE_MOCK) return mockResponse(MOCK_HOST_FILES);
  return http.get('/host/files');
}

/**
 * POST /host/verify-biz — 국세청 사업자등록정보 진위확인
 * 데모에서는 형식(000-00-00000)만 맞으면 통과 처리한다.
 */
export async function verifyBusinessNumber(payload: {
  bizNumber: string;
  companyName: string;
  ceoName: string;
}): Promise<{ verified: boolean; message: string }> {
  if (USE_MOCK) {
    const valid = /^\d{3}-\d{2}-\d{5}$/.test(payload.bizNumber);
    return mockResponse({
      verified: valid,
      message: valid
        ? '국세청 진위확인 완료 · 인증 배지가 부여됐어요.'
        : '사업자등록번호 형식을 확인해 주세요. (000-00-00000)',
    });
  }
  return http.post('/host/verify-biz', payload);
}

/** GET /companies/search?keyword= — 회원가입 시 회사 검색 */
export async function searchCompanies(keyword: string) {
  if (USE_MOCK) {
    const trimmed = keyword.trim();
    if (!trimmed) return mockResponse([]);
    return mockResponse(
      [MOCK_HOST_COMPANY, { ...MOCK_HOST_COMPANY, id: 'paybridge', name: '페이브릿지' }].filter(
        (company) => company.name.includes(trimmed),
      ),
    );
  }
  return http.get('/companies/search', { query: { keyword } });
}
