import type { Contest, ContestDetail, ContestFormat, ContestTag } from '@/lib/types';
import { MOCK_CONTESTS, MOCK_CONTEST_DETAILS, MOCK_HOST_COMPANY } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

export interface ContestQuery {
  /** 공모전 / 해커톤 (기획서 2.4) */
  format?: ContestFormat | '전체';
  /** 분야 7종 (기획서 3.5) */
  tag?: ContestTag | '전체';
}

/** GET /contests */
export async function fetchContests(query: ContestQuery = {}): Promise<Contest[]> {
  if (USE_MOCK) {
    const { format = '전체', tag = '전체' } = query;
    return mockResponse(
      MOCK_CONTESTS.filter(
        (contest) =>
          (format === '전체' || contest.format === format) &&
          (tag === '전체' || contest.tag === tag),
      ),
    );
  }
  return http.get<Contest[]>('/contests', { query: query as Record<string, string> });
}

/** GET /contests/{id} */
export async function fetchContest(id: string): Promise<ContestDetail> {
  if (USE_MOCK) {
    const found = MOCK_CONTEST_DETAILS[id];
    if (found) return mockResponse(found);
    // 없는 id 에 다른 대회 정보를 보여주면 오해를 부르므로 빈 껍데기를 돌려준다
    return mockResponse(emptyContestDetail(id));
  }
  return http.get<ContestDetail>(`/contests/${id}`);
}

function emptyContestDetail(id: string): ContestDetail {
  return {
    id,
    title: '등록된 대회 정보가 없어요',
    host: '-',
    format: 'COMPETITION',
    tag: '기타',
    status: '접수중',
    prize: '-',
    dday: '-',
    period: '-',
    linkUrl: '#',
    viewCount: 0,
    likeCount: 0,
    teams: 0,
    description: '',
    target: '-',
    relatedParties: [],
  };
}

/** 접수 종료일로부터 남은 일수를 D-day 문구로 만든다 */
function toDday(endDate: string): string {
  if (!endDate) return '-';
  const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86_400_000);
  if (Number.isNaN(diff)) return '-';
  return diff > 0 ? `D-${diff}` : diff === 0 ? 'D-DAY' : '마감';
}

const toPeriod = (start: string, end: string) =>
  start && end ? `${start.replace(/-/g, '.')} ~ ${end.replace(/-/g, '.').slice(5)}` : '-';

/** 폼 값을 상세 화면이 그대로 쓰는 모양으로 옮긴다 */
function toContestDetail(id: string, payload: ContestFormPayload): ContestDetail {
  return {
    id,
    title: payload.title,
    host: MOCK_HOST_COMPANY.name,
    format: payload.format,
    tag: payload.tag,
    status: '접수중',
    prize: payload.prize,
    dday: toDday(payload.endDate),
    period: toPeriod(payload.startDate, payload.endDate),
    linkUrl: payload.linkUrl,
    viewCount: 0,
    likeCount: 0,
    teams: 0,
    description: payload.description,
    target: payload.target,
    relatedParties: [],
  };
}

/** GET /contests/search?keyword= — 파티 생성 시 공모전 연동 검색 */
export async function searchContests(keyword: string): Promise<Contest[]> {
  if (USE_MOCK) {
    const trimmed = keyword.trim();
    if (!trimmed) return mockResponse([]);
    return mockResponse(
      MOCK_CONTESTS.filter(
        (contest) => contest.title.includes(trimmed) || contest.host.includes(trimmed),
      ),
    );
  }
  return http.get<Contest[]>('/contests/search', { query: { keyword } });
}

/** POST /contests/{id}/likes, DELETE /contests/{id}/likes (기획서 3.2) */
export async function toggleContestLike(
  id: string,
  liked: boolean,
): Promise<{ likeCount: number }> {
  if (USE_MOCK) {
    const base = MOCK_CONTESTS.find((contest) => contest.id === id)?.likeCount ?? 0;
    return mockResponse({ likeCount: liked ? base + 1 : base });
  }
  return liked
    ? http.post<{ likeCount: number }>(`/contests/${id}/likes`)
    : http.delete<{ likeCount: number }>(`/contests/${id}/likes`);
}

/** POST /contests/{id}/bookmark, DELETE /contests/{id}/bookmark */
export async function toggleContestBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return bookmarked
    ? http.post<void>(`/contests/${id}/bookmark`)
    : http.delete<void>(`/contests/${id}/bookmark`);
}

/**
 * 대회 등록 · 수정 폼 값.
 * 상세 페이지(ContestDetail)가 보여주는 항목과 1:1로 맞춘다 —
 * 여기서 입력한 값이 그대로 상세에 나타나야 한다.
 */
export interface ContestFormPayload {
  title: string;
  /** 공모전 / 해커톤 — 필수 (기획서 2.4) */
  format: ContestFormat;
  /** 분야 — 7종 필수 (기획서 3.5) */
  tag: ContestTag;
  /** 원본 페이지 링크 — 필수 (기획서 3.5) */
  linkUrl: string;
  /** 접수 기간 — 상세의 '접수 2026.08.01 ~ 09.10' 과 D-day 계산에 쓰인다 */
  startDate: string;
  endDate: string;
  coverFileName?: string;
  /** 상세 사이드의 '상금' */
  prize: string;
  /** 상세 사이드의 '참가 대상' */
  target: string;
  /** 상세의 '공모전 소개' — 시상 내역과 일정도 여기에 적는다 */
  description: string;
}

/** POST /contests — 등록 신청(PENDING) */
export async function createContest(payload: ContestFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) {
    const id = `contest-${Date.now()}`;
    // 등록 직후 상세로 이동하므로, 목 저장소에도 넣어야 그 화면에서 입력값이 보인다
    const detail = toContestDetail(id, payload);
    MOCK_CONTEST_DETAILS[id] = detail;
    MOCK_CONTESTS.unshift(detail);
    return mockResponse({ id });
  }
  return http.post<{ id: string }>('/contests', payload);
}

/**
 * DELETE /contests/{id} — 같은 주최측이거나 관리자만 지울 수 있다.
 * 권한 판단은 서버가 최종적으로 하고, 화면은 버튼을 감추는 것까지만 한다.
 */
export async function deleteContest(id: string): Promise<void> {
  if (USE_MOCK) {
    const index = MOCK_CONTESTS.findIndex((contest) => contest.id === id);
    if (index >= 0) MOCK_CONTESTS.splice(index, 1);
    delete MOCK_CONTEST_DETAILS[id];
    return mockResponse(undefined as void);
  }
  return http.delete<void>(`/contests/${id}`);
}

/** PUT /contests/{id} */
export async function updateContest(
  id: string,
  payload: ContestFormPayload,
): Promise<{ id: string }> {
  if (USE_MOCK) {
    const existing = MOCK_CONTEST_DETAILS[id];
    MOCK_CONTEST_DETAILS[id] = {
      ...toContestDetail(id, payload),
      viewCount: existing?.viewCount ?? 0,
      likeCount: existing?.likeCount ?? 0,
      teams: existing?.teams ?? 0,
      relatedParties: existing?.relatedParties ?? [],
    };
    return mockResponse({ id });
  }
  return http.put<{ id: string }>(`/contests/${id}`, payload);
}
