import type { HostApproval, ReportItem } from '@/lib/types';
import {
  MOCK_ADMIN_AWARDS,
  MOCK_ADMIN_BADGES,
  MOCK_ADMIN_HOSTS,
  MOCK_ADMIN_KPIS,
  MOCK_ADMIN_MEMBERS,
  MOCK_ADMIN_PARTIES,
  MOCK_ADMIN_REPORTS,
  MOCK_ADMIN_SIGNUP_CHART,
  MOCK_ADMIN_SIGNUP_TABLE,
} from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

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

/** GET /admin/parties */
export async function fetchAdminParties(keyword = '') {
  if (USE_MOCK) {
    return mockResponse(
      MOCK_ADMIN_PARTIES.filter(
        (row) => !keyword || row.title.includes(keyword) || row.leader.includes(keyword),
      ),
    );
  }
  return http.get('/admin/parties', { query: { keyword } });
}

/** GET /admin/members */
export async function fetchAdminMembers(keyword = '') {
  if (USE_MOCK) {
    return mockResponse(
      MOCK_ADMIN_MEMBERS.filter(
        (row) => !keyword || row.name.includes(keyword) || row.email.includes(keyword),
      ),
    );
  }
  return http.get('/admin/members', { query: { keyword } });
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
