/**
 * 파티 API — ApiV1PartyController / ApiV1PartyApplicationController / ApiV1LikeController 기준.
 *
 * 백엔드 목록은 Page<PartyListItemDto> 로 내려오고 상세는 PartyDto 라서 필드 구성이 다르다.
 * 화면 타입(Party / PartyDetail)으로 옮기는 매퍼를 이 파일에 모아 둔다.
 */
import type {
  Applicant,
  ApplicantStatus,
  ContestFormat,
  Party,
  PartyDetail,
  PartyStatus,
  PositionType,
  TopicType,
  UserSummary,
} from '@/lib/types';
import {
  MOCK_APPLICANTS,
  MOCK_PARTIES,
  MOCK_PARTY_DETAILS,
  MOCK_RECOMMENDED_PARTIES,
} from '@/lib/mock';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

/* ---------- 백엔드 응답 타입 ---------- */

type PartyTag = 'WEB' | 'APP' | 'GAME' | 'ETC';
type ServerPositionType = 'BACK' | 'FRONT' | 'UIUX' | 'PM';

interface PositionResponse {
  id: number;
  type: ServerPositionType;
  capacity: number;
  filledCount: number;
}

/** PartyListItemDto */
interface PartyListItemResponse {
  id: number;
  ownerName: string;
  partyName: string;
  title: string;
  topicType: TopicType;
  status: PartyStatus;
  partyTag: PartyTag;
  deadline: string;
  dDay: number;
  likeCount: number;
  viewCount: number;
  positions: PositionResponse[];
}

/** PartySearchResultDto */
interface PartySearchResponse {
  query: string;
  matchedKeywords: string[];
  content: PartyListItemResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** PartyDto — 상세. 목록보다 필드가 많다 */
interface PartyResponse extends Omit<PartyListItemResponse, 'ownerName'> {
  ownerId: number;
  ownerName: string;
  description: string | null;
  targetContestId: number | null;
  contestName: string | null;
  contestLinkUrl: string | null;
  githubRepoUrl: string | null;
  checklistRequiredApprovals: number;
}

/** PartyApplicationDto */
interface PartyApplicationResponse {
  id: number;
  partyId: number;
  applicantId: number;
  applicantName: string;
  positionId: number;
  positionType: ServerPositionType;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  message: string | null;
  createDate: string;
}

/** LikeDto */
interface LikeResponse {
  targetType: 'PARTY' | 'CONTEST' | 'GOAL';
  targetId: number;
  liked: boolean;
  likeCount: number;
}

/* ---------- 매퍼 ---------- */

/** 화면 PositionType 은 이번 스코프에서 BACK/FRONT 만 쓴다(lib/types.ts). 나머지는 BACK 으로 접는다 */
function toPositionType(value: ServerPositionType): PositionType {
  return value === 'FRONT' ? 'FRONT' : 'BACK';
}

/** 화면 필터가 쓰는 한글 분야 라벨 ↔ 서버 PartyTag */
const TAG_TO_LABEL: Record<PartyTag, string> = {
  WEB: '웹 개발',
  APP: '앱 개발',
  GAME: '게임 개발',
  ETC: '기타',
};

function toPartyTag(label?: string): PartyTag | undefined {
  const found = (Object.keys(TAG_TO_LABEL) as PartyTag[]).find(
    (tag) => TAG_TO_LABEL[tag] === label,
  );
  return found;
}

function toUserSummary(id: string, name: string): UserSummary {
  return { id, name, initial: name.charAt(0) || 'C', role: '' };
}

/**
 * PartyListItemDto → Party.
 *
 * 목록 응답에 없어서 비워 두는 값:
 * - summary, tags   : 서버 목록 DTO 에 본문·태그가 없다
 * - applicants      : 지원자 수 필드가 없다 (filledCount 는 승인 인원이라 다른 값이다)
 * - createdAt       : 서버가 생성일을 내려주지 않는다
 * - leader.id       : 목록 DTO 에 ownerId 가 없다 (상세에는 있다)
 */
function toParty(dto: PartyListItemResponse): Party {
  return {
    id: String(dto.id),
    title: dto.title,
    summary: '',
    topicType: dto.topicType,
    subCategory: TAG_TO_LABEL[dto.partyTag],
    positions: dto.positions.map((position) => ({
      type: toPositionType(position.type),
      capacity: position.capacity,
      filledCount: position.filledCount,
    })),
    applicants: 0,
    dday: dto.dDay >= 0 ? `D-${dto.dDay}` : '마감',
    deadline: dto.deadline,
    createdAt: '',
    leader: toUserSummary('', dto.ownerName),
    tags: [],
    likeCount: dto.likeCount,
    viewCount: dto.viewCount,
    status: dto.status,
  };
}

/**
 * PartyDto → PartyDetail.
 *
 * 서버에 없어서 비워 두는 값:
 * - schedule, meetingType, requirements : 상세 화면 전용 필드 미구현
 * - members : 파티원 목록 API 가 아직 없다 (지원자 목록은 파티장 전용이다)
 * - likedByMe, bookmarkedByMe : PartyDto 에 없다. 대회(ContestDto)에는 있어서 파티만 비어 있다.
 *   그래서 좋아요·북마크 버튼이 항상 꺼진 상태로 시작한다 (docs/프론트-API연동_백엔드_수정요청.md ⑥)
 */
function toPartyDetail(dto: PartyResponse): PartyDetail {
  return {
    ...toParty(dto),
    leader: toUserSummary(String(dto.ownerId), dto.ownerName),
    summary: dto.description ?? '',
    description: dto.description ?? '',
    contestId: dto.targetContestId != null ? String(dto.targetContestId) : undefined,
    contestName: dto.contestName ?? undefined,
    contestLinkUrl: dto.contestLinkUrl ?? undefined,
    githubRepoUrl: dto.githubRepoUrl ?? undefined,
    checklistRequiredApprovals: dto.checklistRequiredApprovals,
    schedule: '',
    meetingType: '',
    members: [],
    requirements: [],
  };
}

const APPLICANT_STATUS: Record<PartyApplicationResponse['status'], ApplicantStatus> = {
  PENDING: 'pending',
  APPROVED: 'accepted',
  REJECTED: 'rejected',
};

/**
 * PartyApplicationDto → Applicant.
 * skills·achievements 는 지원자 성취 프로필 조회가 아직 응답에 없어 비워 둔다(기획서 9.2).
 */
function toApplicant(dto: PartyApplicationResponse, partyName = ''): Applicant {
  return {
    id: String(dto.id),
    partyId: String(dto.partyId),
    partyName,
    position: toPositionType(dto.positionType),
    source: 'SELF_REPORTED',
    user: toUserSummary(String(dto.applicantId), dto.applicantName),
    appliedAt: dto.createDate,
    status: APPLICANT_STATUS[dto.status],
    message: dto.message ?? '',
    skills: [],
    achievements: [],
  };
}

/* ---------- 목록 · 상세 ---------- */

export interface PartyQuery {
  keyword?: string;
  /** 주제 유형 — 서버 목록 API 에 필터가 없어 화면에서 거른다 */
  topicType?: TopicType | '전체';
  /** 분야 — 서버 PartyTag(WEB/APP/GAME/ETC)로 변환해 보낸다 */
  subCategory?: string;
  /** 포지션 필터 (기획서 2.1) */
  position?: PositionType | '전체';
  /** 빈 자리 많은 순 / 마감 임박 순 / 인기순(좋아요 수) (기획서 2.1) */
  sort?: 'empty' | 'dday' | 'like';
  page?: number;
  size?: number;
}

const SORT_TO_SERVER: Record<'empty' | 'dday' | 'like', string> = {
  empty: 'VACANCY',
  dday: 'DEADLINE',
  like: 'POPULAR',
};

/** GET /api/v1/parties — 서버는 Page 로 감싸 내려주므로 content 만 꺼내 쓴다 */
export async function fetchParties(query: PartyQuery = {}): Promise<Party[]> {
  if (USE_MOCK) {
    const {
      keyword = '',
      topicType = '전체',
      subCategory = '전체',
      position = '전체',
      sort = 'empty',
    } = query;

    const filtered = MOCK_PARTIES.filter((party) => {
      const matchKeyword = !keyword || party.title.includes(keyword);
      const matchTopic = topicType === '전체' || party.topicType === topicType;
      const matchSub = subCategory === '전체' || party.subCategory === subCategory;
      const matchPosition =
        position === '전체' || party.positions.some((slot) => slot.type === position);
      return matchKeyword && matchTopic && matchSub && matchPosition;
    });

    return mockResponse([...filtered].sort(comparePartiesBy(sort)));
  }

  // 기획서 9.2 는 파티 목록을 비인증 조회로 정의하지만 서버가 아직 인증을 요구한다.
  // 비로그인 방문자에게 페이지 전체가 500 으로 죽는 것보다는 빈 목록이 낫다.
  // 서버 permitAll 이 반영되면 이 방어는 걷어낸다 (docs/프론트-API연동_백엔드_수정요청.md ②)
  let page: { content: PartyListItemResponse[] };
  try {
    page = await http.get<{ content: PartyListItemResponse[] }>('/parties', {
      query: {
        keyword: query.keyword,
        partyTag: toPartyTag(query.subCategory),
        position: query.position === '전체' ? undefined : query.position,
        sort: SORT_TO_SERVER[query.sort ?? 'empty'],
        page: query.page ?? 0,
        size: query.size ?? 20,
      },
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return [];
    throw error;
  }

  const parties = page.content.map(toParty);
  // 서버 목록 API 에 주제 유형 필터가 없어 여기서 거른다
  return query.topicType && query.topicType !== '전체'
    ? parties.filter((party) => party.topicType === query.topicType)
    : parties;
}

/** 목록 정렬 기준 — 화면과 목 API 가 같은 규칙을 쓰도록 공유한다 */
export function comparePartiesBy(sort: 'empty' | 'dday' | 'like') {
  return (a: Party, b: Party) => {
    if (sort === 'dday') {
      return ddayValue(a.dday) - ddayValue(b.dday);
    }
    if (sort === 'like') {
      return b.likeCount - a.likeCount;
    }
    return emptySlots(b) - emptySlots(a);
  };
}

const emptySlots = (party: Party) =>
  party.positions.reduce((sum, slot) => sum + (slot.capacity - slot.filledCount), 0);

/** 'D-3' → 3, '완료' 처럼 숫자가 없으면 맨 뒤로 보낸다 */
const ddayValue = (dday: string) => {
  const parsed = Number(dday.replace('D-', ''));
  return Number.isNaN(parsed) ? Number.MAX_SAFE_INTEGER : parsed;
};

/**
 * 성취 키워드 기반 추천.
 * 백엔드 추천 API(기획서 9.6)가 아직 없어 데모 데이터를 그대로 쓴다.
 */
export async function fetchRecommendedParties(): Promise<(Party & { why: string })[]> {
  return mockResponse(MOCK_RECOMMENDED_PARTIES);
}

/** GET /api/v1/parties/search — 형태소 분석 기반 유사 파티 검색 (keyword 필터와 별개) */
export async function fetchPartySearch(
  query: string,
  options: { page?: number; size?: number } = {},
): Promise<Party[]> {
  const q = query.trim();
  if (!q) return [];

  if (USE_MOCK) {
    return mockResponse(MOCK_PARTIES.filter((party) => party.title.includes(q)));
  }

  try {
    const result = await http.get<PartySearchResponse>('/parties/search', {
      query: { q, page: options.page ?? 0, size: options.size ?? 20 },
    });
    return result.content.map(toParty);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400) return [];
    throw error;
  }
}

/** GET /api/v1/parties/{partyId} */
export async function fetchParty(id: string): Promise<PartyDetail> {
  if (USE_MOCK) {
    const detail = MOCK_PARTY_DETAILS[id] ?? MOCK_PARTY_DETAILS.oakroom;
    return mockResponse(detail);
  }
  return toPartyDetail(await http.get<PartyResponse>(`/parties/${id}`));
}

/* ---------- 생성 · 수정 · 삭제 ---------- */

export interface PartyFormPayload {
  topicType: TopicType;
  /** topicType 이 CONTEST 일 때만 사용 */
  contestFormat?: ContestFormat;
  contestId?: string;
  contestName?: string;
  /** 원본 대회 링크 — 등록 여부와 무관하게 항상 받는다 (기획서 3.5) */
  contestLinkUrl?: string;
  title: string;
  description: string;
  /** 대표 사진 1장 — 목록 카드·상세 상단에 쓰인다 */
  coverFileName?: string;
  positions: { type: PositionType; capacity: number }[];
  repositoryUrl?: string;

  /**
   * 아래 셋은 서버가 필수로 받는 값인데 파티 생성 폼이 아직 입력받지 않는다.
   * 넘기지 않으면 임시값으로 채워 보내므로, 폼에 입력칸을 추가하는 게 맞다.
   */
  partyName?: string;
  /** 분야 — 화면 라벨('웹 개발' 등) 그대로 넘기면 서버 enum 으로 변환한다 */
  subCategory?: string;
  /** 모집 기한 ISO 문자열. 없으면 30일 뒤로 잡는다 */
  deadline?: string;
}

function toPartyRequestBody(payload: PartyFormPayload) {
  const deadline =
    payload.deadline ?? new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19);

  return {
    // 서버는 파티 이름과 모집글 제목을 따로 받는다. 폼에 이름 칸이 없어 제목을 그대로 쓴다.
    partyName: payload.partyName ?? payload.title,
    title: payload.title,
    description: payload.description,
    targetContestId: payload.contestId ? Number(payload.contestId) : null,
    contestName: payload.contestName ?? null,
    contestLinkUrl: payload.contestLinkUrl ?? null,
    topicType: payload.topicType,
    partyTag: toPartyTag(payload.subCategory) ?? 'ETC',
    githubRepoUrl: payload.repositoryUrl ?? null,
    // 서버 요청 record 의 int 필드라 값을 빼면 본문 파싱 자체가 실패한다(400-2).
    // 진행 기록 방식이 커밋 기반으로 바뀌는 중이라 폼에 입력칸이 없어, 최소값 1 로 보낸다.
    checklistRequiredApprovals: 1,
    deadline,
    positions: payload.positions.map((position) => ({
      name: position.type,
      capacity: position.capacity,
    })),
  };
}

/** POST /api/v1/parties */
export async function createParty(payload: PartyFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id: `party-${Date.now()}` });

  const created = await http.post<PartyResponse>('/parties', toPartyRequestBody(payload));
  return { id: String(created.id) };
}

/**
 * PATCH /api/v1/parties/{partyId} (PUT 아님)
 *
 * 서버 수정 API 는 포지션을 positionId 기준으로 정원만 바꾼다.
 * 폼은 포지션 종류로만 들고 있어 정원 수정은 보내지 않는다 — 정원 변경 UI 가 붙으면 함께 채워야 한다.
 */
export async function updateParty(id: string, payload: PartyFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id });

  const { positions: _positions, ...body } = toPartyRequestBody(payload);
  const updated = await http.patch<PartyResponse>(`/parties/${id}`, body);
  return { id: String(updated.id) };
}

/**
 * DELETE /api/v1/parties/{partyId} — 파티장만 지울 수 있다.
 * 권한 판단은 서버가 최종적으로 하고, 화면은 버튼을 감추는 것까지만 한다.
 */
export async function deleteParty(id: string): Promise<void> {
  if (USE_MOCK) {
    const index = MOCK_PARTIES.findIndex((party) => party.id === id);
    if (index >= 0) MOCK_PARTIES.splice(index, 1);
    delete MOCK_PARTY_DETAILS[id];
    return mockResponse(undefined as void);
  }
  await http.delete<void>(`/parties/${id}`);
}

/** POST /api/v1/parties/{partyId}/close-recruiting — 모집 마감(파티 확정) */
export async function closePartyRecruit(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<PartyResponse>(`/parties/${id}/close-recruiting`);
}

/** POST /api/v1/parties/{partyId}/complete — 파티 완료 판정 */
export async function completeParty(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<PartyResponse>(`/parties/${id}/complete`);
}

/* ---------- 지원 · 승인 ---------- */

/**
 * POST /api/v1/parties/{partyId}/applications
 *
 * 서버는 포지션 종류가 아니라 positionId 를 받는다.
 * 화면 PartyPosition 에는 id 가 없어, 상세를 한 번 읽어 포지션 종류로 id 를 찾는다.
 */
export async function applyToParty(
  id: string,
  payload: { position: string; message: string },
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);

  const party = await http.get<PartyResponse>(`/parties/${id}`);
  const matched = party.positions.find((position) => position.type === payload.position);
  if (!matched) throw new Error('해당 포지션을 찾을 수 없습니다.');

  await http.post<void>(`/parties/${id}/applications`, {
    positionId: matched.id,
    message: payload.message,
  });
}

/**
 * 내가 파티장인 파티의 지원자.
 *
 * 서버는 파티 단위(GET /parties/{partyId}/applications)로만 내려준다.
 * 여러 파티를 한 번에 모으는 API(기획서 9.11)가 없어, partyId 가 있을 때만 실제로 조회한다.
 */
export async function fetchMyPartyApplicants(
  query: { partyId?: string; position?: PositionType | '전체' } = {},
): Promise<Applicant[]> {
  const { partyId = '전체', position = '전체' } = query;

  if (USE_MOCK || partyId === '전체' || !partyId) {
    return mockResponse(
      MOCK_APPLICANTS.filter(
        (applicant) =>
          (partyId === '전체' || applicant.partyId === partyId) &&
          (position === '전체' || applicant.position === position),
      ),
    );
  }

  const applications = await http.get<PartyApplicationResponse[]>(
    `/parties/${partyId}/applications`,
  );
  return applications
    .map((application) => toApplicant(application))
    .filter((applicant) => position === '전체' || applicant.position === position);
}

/**
 * PATCH /api/v1/parties/{partyId}/applications/{applicationId} — 승인 · 거절
 * 서버 경로가 파티에 종속돼 있어 partyId 가 함께 필요하다.
 */
export async function decideApplicant(
  id: string,
  status: ApplicantStatus,
  partyId?: string,
): Promise<void> {
  if (USE_MOCK || !partyId) return mockResponse(undefined as void);

  await http.patch<PartyApplicationResponse>(`/parties/${partyId}/applications/${id}`, {
    pending: status === 'accepted' ? 'APPROVED' : 'REJECTED',
  });
}

/**
 * 내가 지원한 내역.
 * 백엔드에 GET /mypage/applications(기획서 9.11)가 아직 없어 데모 데이터를 그대로 쓴다.
 */
export async function fetchMyApplications(): Promise<Applicant[]> {
  return mockResponse([
    {
      ...MOCK_APPLICANTS[0],
      id: 'my-1',
      partyId: 'commerce-clone',
      partyName: '커머스 클론 사이드프로젝트',
      position: 'BACK' as PositionType,
      message: '성취 프로필 전체 첨부됨',
    },
  ]);
}

/* ---------- 좋아요 · 북마크 ---------- */

/**
 * POST /api/v1/parties/{partyId}/likes, DELETE 로 취소.
 * (memberId, targetType, targetId) 유니크 제약이 있어 여러 번 눌러도 상태가 어긋나지 않는다(기획서 3.2).
 */
export async function togglePartyLike(id: string, liked: boolean): Promise<{ likeCount: number }> {
  if (USE_MOCK) {
    const base = MOCK_PARTIES.find((party) => party.id === id)?.likeCount ?? 0;
    return mockResponse({ likeCount: liked ? base + 1 : base });
  }

  if (liked) {
    const result = await http.post<LikeResponse>(`/parties/${id}/likes`);
    return { likeCount: result.likeCount };
  }

  // 취소 응답에는 본문이 없어 최신 수를 상세에서 다시 읽는다
  await http.delete<void>(`/parties/${id}/likes`);
  const party = await http.get<PartyResponse>(`/parties/${id}`);
  return { likeCount: party.likeCount };
}

/**
 * 파티 북마크. POST /api/v1/parties/{partyId}/bookmarks, DELETE 로 취소 (경로가 복수형이다).
 *
 * 서버는 이미 북마크한 파티를 또 북마크하면 409-1 로 거절한다. 그런데 파티 상세 응답에
 * bookmarkedByMe 가 없어서(toPartyDetail 주석 참고) 버튼은 늘 꺼진 상태로 시작한다 —
 * 이미 북마크해 둔 파티에서 누르면 409 가 나고 버튼만 되돌아간다.
 * 409 는 이미 원하는 상태라는 뜻이므로 성공으로 본다. 서버가 상태를 내려주면 이 방어는 걷어낸다.
 */
export async function togglePartyBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);

  try {
    if (bookmarked) {
      await http.post<{ bookmarked: boolean }>(`/parties/${id}/bookmarks`);
      return;
    }
    await http.delete<void>(`/parties/${id}/bookmarks`);
  } catch (error) {
    // 409-1 — 북마크 중복 / 없는 북마크 취소. 어느 쪽이든 화면이 바라는 상태와 결과가 같다
    if (error instanceof ApiError && error.status === 409) return;
    throw error;
  }
}
