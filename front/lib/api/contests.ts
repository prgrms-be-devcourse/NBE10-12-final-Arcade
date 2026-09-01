/**
 * 대회(공모전·해커톤) API — ApiV1ContestController / ApiV1LikeController / ApiV1BookmarkController 기준.
 *
 * 서버는 Contest(대회 정보)와 ContestPost(게시글)를 나눠 저장하고 ContestResponseDto 하나로 합쳐 내려준다.
 * 게시글이 없는 대회는 archived=true 로 오고 description·linkUrl·likeCount 가 전부 null 이다.
 */
import type { Contest, ContestDetail, ContestFormat, ContestTag } from '@/lib/types';
import { MOCK_CONTESTS, MOCK_CONTEST_DETAILS, MOCK_HOST_COMPANY } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

/* ---------- 백엔드 응답 타입 ---------- */

type ServerContestFormat = 'CONTEST' | 'HACKATHON';
type ServerContestTag = 'DATA' | 'ENVIRONMENT' | 'FINTECH' | 'UX' | 'AI' | 'LOCAL_ECONOMY' | 'ETC';

/** ContestResponseDto */
interface ContestResponse {
  id: number;
  hostId: number | null;
  creatorMemberId: number | null;
  title: string;
  format: ServerContestFormat;
  contestTag: ServerContestTag;
  applicationPeriodStart: string;
  applicationPeriodEnd: string;
  /** 게시글(ContestPost)이 아직 없는 대회 */
  archived: boolean;
  description: string | null;
  imageUrl: string | null;
  linkUrl: string | null;
  likeCount: number | null;
  viewCount: number | null;
  bookmarkedByMe: boolean;
  likedByMe: boolean;
  createDate: string;
}

interface LikeResponse {
  targetType: 'PARTY' | 'CONTEST' | 'GOAL';
  targetId: number;
  liked: boolean;
  likeCount: number;
}

/* ---------- enum 변환 ---------- */

/** 화면은 '공모전'을 COMPETITION 으로, 서버는 CONTEST 로 부른다 */
const FORMAT_TO_SERVER: Record<ContestFormat, ServerContestFormat> = {
  COMPETITION: 'CONTEST',
  HACKATHON: 'HACKATHON',
};
const FORMAT_TO_CLIENT: Record<ServerContestFormat, ContestFormat> = {
  CONTEST: 'COMPETITION',
  HACKATHON: 'HACKATHON',
};

/** 화면은 한글 라벨을, 서버는 enum 을 쓴다 (기획서 3.5) */
const TAG_TO_SERVER: Record<ContestTag, ServerContestTag> = {
  데이터: 'DATA',
  환경: 'ENVIRONMENT',
  핀테크: 'FINTECH',
  UX: 'UX',
  AI: 'AI',
  지역경제: 'LOCAL_ECONOMY',
  기타: 'ETC',
};
const TAG_TO_CLIENT: Record<ServerContestTag, ContestTag> = {
  DATA: '데이터',
  ENVIRONMENT: '환경',
  FINTECH: '핀테크',
  UX: 'UX',
  AI: 'AI',
  LOCAL_ECONOMY: '지역경제',
  ETC: '기타',
};

/* ---------- 매퍼 ---------- */

/** 접수 종료일로부터 남은 일수를 D-day 문구로 만든다 */
function toDday(endDate: string): string {
  if (!endDate) return '-';
  const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86_400_000);
  if (Number.isNaN(diff)) return '-';
  return diff > 0 ? `D-${diff}` : diff === 0 ? 'D-DAY' : '마감';
}

const toPeriod = (start: string, end: string) =>
  start && end ? `${start.replace(/-/g, '.')} ~ ${end.replace(/-/g, '.').slice(5)}` : '-';

/** 접수 마감일로 카드 뱃지 문구를 정한다. 서버에 상태 필드가 따로 없다 */
function toStatus(endDate: string): Contest['status'] {
  const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86_400_000);
  if (Number.isNaN(diff)) return '접수중';
  if (diff < 0) return '마감';
  return diff <= 7 ? '마감임박' : '접수중';
}

/**
 * ContestResponseDto → Contest.
 *
 * 서버에 없어서 비워 두는 값:
 * - host  : 주최측 이름이 응답에 없다. hostId·creatorMemberId 만 온다
 * - teams : 참가팀 수 필드가 없다
 */
function toContest(dto: ContestResponse): Contest {
  return {
    id: String(dto.id),
    title: dto.title,
    host: '',
    hostId: dto.hostId != null ? String(dto.hostId) : undefined,
    format: FORMAT_TO_CLIENT[dto.format],
    tag: TAG_TO_CLIENT[dto.contestTag],
    status: toStatus(dto.applicationPeriodEnd),
    dday: toDday(dto.applicationPeriodEnd),
    period: toPeriod(dto.applicationPeriodStart, dto.applicationPeriodEnd),
    linkUrl: dto.linkUrl ?? '#',
    coverImageUrl: dto.imageUrl ?? undefined,
    viewCount: dto.viewCount ?? 0,
    likeCount: dto.likeCount ?? 0,
    likedByMe: dto.likedByMe,
    bookmarkedByMe: dto.bookmarkedByMe,
    teams: 0,
  };
}

/**
 * ContestResponseDto → ContestDetail.
 * relatedParties(참가팀 목록, 기획서 9.5)는 상세 응답에 아직 없어 비워 둔다.
 */
function toContestDetailResponse(dto: ContestResponse): ContestDetail {
  return {
    ...toContest(dto),
    description: dto.description ?? '',
    relatedParties: [],
  };
}

/* ---------- 목록 · 상세 ---------- */

export interface ContestQuery {
  /** 공모전 / 해커톤 (기획서 2.4) */
  format?: ContestFormat | '전체';
  /** 분야 7종 (기획서 3.5) */
  tag?: ContestTag | '전체';
  /** 최신순 / 인기순 / 마감임박순 */
  sort?: 'LATEST' | 'POPULAR' | 'DEADLINE';
  page?: number;
  size?: number;
}

/** GET /api/v1/contests — 서버는 Page 로 감싸 내려주므로 content 만 꺼내 쓴다 */
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

  const page = await http.get<{ content: ContestResponse[] }>('/contests', {
    query: {
      format: query.format && query.format !== '전체' ? FORMAT_TO_SERVER[query.format] : undefined,
      contestTag: query.tag && query.tag !== '전체' ? TAG_TO_SERVER[query.tag] : undefined,
      sort: query.sort ?? 'LATEST',
      page: query.page ?? 0,
      size: query.size ?? 20,
    },
  });
  return page.content.map(toContest);
}

/** GET /api/v1/contests/{contestId} */
export async function fetchContest(id: string): Promise<ContestDetail> {
  if (USE_MOCK) {
    const found = MOCK_CONTEST_DETAILS[id];
    if (found) return mockResponse(found);
    // 없는 id 에 다른 대회 정보를 보여주면 오해를 부르므로 빈 껍데기를 돌려준다
    return mockResponse(emptyContestDetail(id));
  }
  return toContestDetailResponse(await http.get<ContestResponse>(`/contests/${id}`));
}

function emptyContestDetail(id: string): ContestDetail {
  return {
    id,
    title: '등록된 대회 정보가 없어요',
    host: '-',
    format: 'COMPETITION',
    tag: '기타',
    status: '접수중',
    dday: '-',
    period: '-',
    linkUrl: '#',
    viewCount: 0,
    likeCount: 0,
    teams: 0,
    description: '',
    relatedParties: [],
  };
}

/** 폼 값을 상세 화면이 그대로 쓰는 모양으로 옮긴다 (목 모드 전용) */
function toContestDetail(id: string, payload: ContestFormPayload): ContestDetail {
  return {
    id,
    title: payload.title,
    host: MOCK_HOST_COMPANY.name,
    format: payload.format,
    tag: payload.tag,
    status: '접수중',
    dday: toDday(payload.endDate),
    period: toPeriod(payload.startDate, payload.endDate),
    linkUrl: payload.linkUrl,
    viewCount: 0,
    likeCount: 0,
    teams: 0,
    description: payload.description,
    relatedParties: [],
  };
}

/**
 * 파티 생성 시 붙일 대회 검색.
 * 서버 목록 API 에 키워드 파라미터가 없어, 한 페이지를 받아 제목으로 거른다.
 */
export async function searchContests(keyword: string): Promise<Contest[]> {
  const trimmed = keyword.trim();
  if (!trimmed) return USE_MOCK ? mockResponse([]) : [];

  if (USE_MOCK) {
    return mockResponse(
      MOCK_CONTESTS.filter(
        (contest) => contest.title.includes(trimmed) || contest.host.includes(trimmed),
      ),
    );
  }

  const contests = await fetchContests({ size: 100 });
  return contests.filter((contest) => contest.title.includes(trimmed));
}

/* ---------- 등록 · 수정 · 삭제 (관리자 전용) ---------- */

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
  /** 상세의 '공모전 소개' — 상금·시상 내역·참가 대상도 여기에 함께 적는다. 서버 검증상 10자 이상 */
  description: string;
}

/**
 * POST /api/v1/contests — 관리자만 등록할 수 있다(기획서 3.4).
 *
 * 서버 검증: title 5~25자, description 10~20000자, linkUrl 필수.
 * coverFileName 은 서버에 대응 필드가 없어 전송하지 않는다.
 */
export async function createContest(payload: ContestFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) {
    const id = `contest-${Date.now()}`;
    // 등록 직후 상세로 이동하므로, 목 저장소에도 넣어야 그 화면에서 입력값이 보인다
    const detail = toContestDetail(id, payload);
    MOCK_CONTEST_DETAILS[id] = detail;
    MOCK_CONTESTS.unshift(detail);
    return mockResponse({ id });
  }

  const created = await http.post<ContestResponse>('/contests', {
    title: payload.title,
    format: FORMAT_TO_SERVER[payload.format],
    contestTag: TAG_TO_SERVER[payload.tag],
    applicationPeriodStart: payload.startDate,
    applicationPeriodEnd: payload.endDate,
    description: payload.description,
    imageUrl: null,
    linkUrl: payload.linkUrl,
  });
  return { id: String(created.id) };
}

/**
 * PATCH /api/v1/contests/{contestId} (PUT 아님)
 * 서버 수정 API 는 format·contestTag 를 받지 않는다 — 형식·분야는 등록 후 바꿀 수 없다.
 */
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

  const updated = await http.patch<ContestResponse>(`/contests/${id}`, {
    title: payload.title,
    description: payload.description,
    applicationPeriodStart: payload.startDate,
    applicationPeriodEnd: payload.endDate,
    linkUrl: payload.linkUrl,
    imageUrl: null,
  });
  return { id: String(updated.id) };
}

/**
 * DELETE /api/v1/contests/{contestId} — 관리자만 지울 수 있다.
 * 권한 판단은 서버가 최종적으로 하고, 화면은 버튼을 감추는 것까지만 한다.
 */
export async function deleteContest(id: string): Promise<void> {
  if (USE_MOCK) {
    const index = MOCK_CONTESTS.findIndex((contest) => contest.id === id);
    if (index >= 0) MOCK_CONTESTS.splice(index, 1);
    delete MOCK_CONTEST_DETAILS[id];
    return mockResponse(undefined as void);
  }
  await http.delete<void>(`/contests/${id}`);
}

/* ---------- 좋아요 · 북마크 ---------- */

/** POST /api/v1/contests/{contestId}/likes, DELETE 로 취소 (기획서 3.2) */
export async function toggleContestLike(
  id: string,
  liked: boolean,
): Promise<{ likeCount: number }> {
  if (USE_MOCK) {
    const base = MOCK_CONTESTS.find((contest) => contest.id === id)?.likeCount ?? 0;
    return mockResponse({ likeCount: liked ? base + 1 : base });
  }

  if (liked) {
    const result = await http.post<LikeResponse>(`/contests/${id}/likes`);
    return { likeCount: result.likeCount };
  }

  // 취소 응답에는 본문이 없어 최신 수를 상세에서 다시 읽는다
  await http.delete<void>(`/contests/${id}/likes`);
  const contest = await http.get<ContestResponse>(`/contests/${id}`);
  return { likeCount: contest.likeCount ?? 0 };
}

/** POST /api/v1/contests/{contestId}/bookmarks, DELETE 로 취소 (경로가 복수형이다) */
export async function toggleContestBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);

  if (bookmarked) {
    await http.post<{ bookmarked: boolean }>(`/contests/${id}/bookmarks`);
    return;
  }
  await http.delete<void>(`/contests/${id}/bookmarks`);
}
