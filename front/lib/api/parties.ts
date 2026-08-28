import type {
  Applicant,
  ApplicantStatus,
  ContestFormat,
  Party,
  PartyDetail,
  PositionType,
  TopicType,
} from '@/lib/types';
import {
  MOCK_APPLICANTS,
  MOCK_PARTIES,
  MOCK_PARTY_DETAILS,
  MOCK_RECOMMENDED_PARTIES,
} from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

export interface PartyQuery {
  keyword?: string;
  /** 주제 유형 — 공모전·해커톤은 CONTEST 하나로 묶인다 */
  topicType?: TopicType | '전체';
  /** 분야 — 기획서 미확정 항목, 잠정 유지 */
  subCategory?: string;
  /** 포지션 필터 (기획서 2.1) */
  position?: PositionType | '전체';
  /** 빈 자리 많은 순 / 마감 임박 순 / 인기순(좋아요 수) (기획서 2.1) */
  sort?: 'empty' | 'dday' | 'like';
}

/** GET /parties */
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
  return http.get<Party[]>('/parties', { query: query as Record<string, string> });
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

/** GET /parties/recommended — 성취 키워드 기반 추천 */
export async function fetchRecommendedParties(): Promise<(Party & { why: string })[]> {
  if (USE_MOCK) return mockResponse(MOCK_RECOMMENDED_PARTIES);
  return http.get<(Party & { why: string })[]>('/parties/recommended');
}

/** GET /parties/{id} */
export async function fetchParty(id: string): Promise<PartyDetail> {
  if (USE_MOCK) {
    const detail = MOCK_PARTY_DETAILS[id] ?? MOCK_PARTY_DETAILS.oakroom;
    return mockResponse(detail);
  }
  return http.get<PartyDetail>(`/parties/${id}`);
}

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
}

/** POST /parties */
export async function createParty(payload: PartyFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id: `party-${Date.now()}` });
  return http.post<{ id: string }>('/parties', payload);
}

/** PUT /parties/{id} */
export async function updateParty(id: string, payload: PartyFormPayload): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id });
  return http.put<{ id: string }>(`/parties/${id}`, payload);
}

/**
 * DELETE /parties/{id} — 파티장만 지울 수 있다.
 * 권한 판단은 서버가 최종적으로 하고, 화면은 버튼을 감추는 것까지만 한다.
 */
export async function deleteParty(id: string): Promise<void> {
  if (USE_MOCK) {
    const index = MOCK_PARTIES.findIndex((party) => party.id === id);
    if (index >= 0) MOCK_PARTIES.splice(index, 1);
    delete MOCK_PARTY_DETAILS[id];
    return mockResponse(undefined as void);
  }
  return http.delete<void>(`/parties/${id}`);
}

/** POST /parties/{id}/close — 인원 모집 완료 */
export async function closePartyRecruit(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>(`/parties/${id}/close`);
}

/** POST /parties/{id}/applications */
export async function applyToParty(
  id: string,
  payload: { position: string; message: string },
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>(`/parties/${id}/applications`, payload);
}

/**
 * POST /parties/{id}/likes, DELETE /parties/{id}/likes
 * 토글 on 은 LIKE 행 추가, off 는 삭제 — (memberId, targetType, targetId) 유니크 제약으로
 * 여러 번 눌러도 상태가 어긋나지 않는다 (기획서 3.2).
 */
export async function togglePartyLike(id: string, liked: boolean): Promise<{ likeCount: number }> {
  if (USE_MOCK) {
    const base = MOCK_PARTIES.find((party) => party.id === id)?.likeCount ?? 0;
    return mockResponse({ likeCount: liked ? base + 1 : base });
  }
  return liked
    ? http.post<{ likeCount: number }>(`/parties/${id}/likes`)
    : http.delete<{ likeCount: number }>(`/parties/${id}/likes`);
}

/** POST /parties/{id}/bookmark, DELETE /parties/{id}/bookmark */
export async function togglePartyBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return bookmarked
    ? http.post<void>(`/parties/${id}/bookmark`)
    : http.delete<void>(`/parties/${id}/bookmark`);
}

/** GET /me/parties/applicants — 내가 파티장인 파티의 지원자 */
export async function fetchMyPartyApplicants(query: {
  partyId?: string;
  position?: PositionType | '전체';
} = {}): Promise<Applicant[]> {
  if (USE_MOCK) {
    const { partyId = '전체', position = '전체' } = query;
    return mockResponse(
      MOCK_APPLICANTS.filter(
        (applicant) =>
          (partyId === '전체' || applicant.partyId === partyId) &&
          (position === '전체' || applicant.position === position),
      ),
    );
  }
  return http.get<Applicant[]>('/me/parties/applicants', { query });
}

/** PATCH /applications/{id} — 승인 · 거절 */
export async function decideApplicant(id: string, status: ApplicantStatus): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/applications/${id}`, { status });
}

/** GET /me/applications — 내가 지원한 내역 */
export async function fetchMyApplications(): Promise<Applicant[]> {
  if (USE_MOCK) {
    return mockResponse([
      {
        ...MOCK_APPLICANTS[0],
        id: 'my-1',
        partyId: 'commerce-clone',
        partyName: '커머스 클론 사이드프로젝트',
        position: 'BACK',
        message: '성취 프로필 전체 첨부됨',
      },
    ]);
  }
  return http.get<Applicant[]>('/me/applications');
}
