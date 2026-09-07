import type {
  ExhibitionComment,
  ExhibitionDetail,
  ExhibitionProject,
  GoalSource,
  GoalStatus,
  GoalType,
} from '@/lib/types';
import {
  MOCK_EXHIBITIONS,
  MOCK_EXHIBITION_DETAILS,
  MOCK_EXHIBITION_COMMITS,
  MOCK_PROFILES,
} from '@/lib/mock';
import { GOAL_TYPE_LABELS } from '@/lib/constants';
import { USE_MOCK as USE_API_MOCK, http, mockResponse } from './client';

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


/** 서버 ShowcaseGoalDto — 전시관 목록의 한 칸 */
interface ShowcaseGoalResponse {
  id: number;
  /** PROJECT 만 채워진다. 개인 성취(CONTEST·CHECKLIST)는 파티가 없다 */
  party: { id: number; name: string } | null;
  type: GoalType;
  status: GoalStatus;
  source: GoalSource;
  detail: { title: string | null };
  likeCount: number;
  createAt: string;
}

/**
 * 서버 전시 목록에 없어서 비워 두는 값:
 * - summary, skills, coverImageUrl : ShowcaseGoalDto 에 본문·기술스택·이미지가 없다
 * - viewCount                      : 목록 응답에 없다 (파티 전시 상세에는 있다)
 * - leader                         : 소유자 정보가 아예 없어 카드에서 생략된다
 * - likedByMe, bookmarkedByMe      : 응답에 없다 (docs/마이페이지-요약API_백엔드_요청.md ⑤)
 *
 * category 는 서버의 분야 태그가 아니라 성취 타입 문구다 - 화면 필터도 이 기준으로 맞춘다.
 */
function toExhibitionProject(dto: ShowcaseGoalResponse): ExhibitionProject {
  return {
    id: String(dto.id),
    title: dto.detail.title ?? '',
    summary: '',
    partyName: dto.party?.name ?? '',
    role: 'BACK',
    category: GOAL_TYPE_LABELS[dto.type],
    source: dto.source,
    skills: [],
    viewCount: 0,
    likeCount: dto.likeCount,
    sourcePartyId: dto.party ? String(dto.party.id) : undefined,
    thumbnailLabel: GOAL_TYPE_LABELS[dto.type],
  };
}

/**
 * GET /api/v1/showcase/goals — 전시 성취 목록 (ARC-96).
 *
 * 서버는 status=ACHIEVED 인 성취만 내려주고, PROJECT 는 파티장이 전시글을 게시해
 * partyShowcase 가 연결된 것만 포함한다. 좋아요/북마크 가능 조건과 같은 기준이다.
 *
 * 분야(category) 필터는 서버에 없어 화면에서 성취 타입으로 거른다.
 */
export async function fetchExhibitions(
  category = '전체',
  sort: 'like' | 'recent' = 'like',
): Promise<ExhibitionProject[]> {
  if (USE_API_MOCK) {
    const filtered =
      category === '전체'
        ? MOCK_EXHIBITIONS
        : MOCK_EXHIBITIONS.filter((project) => project.category === category);
    const sorted =
      sort === 'like' ? [...filtered].sort((a, b) => b.likeCount - a.likeCount) : filtered;
    return mockResponse(sorted);
  }

  const page = await http.get<{ content: ShowcaseGoalResponse[] }>('/showcase/goals', {
    query: { sort: sort === 'like' ? 'POPULAR' : 'LATEST', size: 60 },
  });
  return page.content.map(toExhibitionProject);
}

/** GET /exhibitions/{id} */
export async function fetchExhibition(id: string): Promise<ExhibitionDetail> {
  if (USE_MOCK) {
    return mockResponse(MOCK_EXHIBITION_DETAILS[id] ?? MOCK_EXHIBITION_DETAILS['settlement-api']);
  }
  return http.get<ExhibitionDetail>(`/exhibitions/${id}`);
}

/**
 * GET /exhibitions/{id}/commits — 완료 시점 커밋 스냅샷.
 * 팀 스페이스의 커밋 내역을 완료 시점 그대로 얼려 보여준다.
 */
export async function fetchExhibitionCommits(id: string) {
  if (USE_MOCK) return mockResponse(MOCK_EXHIBITION_COMMITS[id] ?? []);
  return http.get(`/exhibitions/${id}/commits`);
}

/** POST /exhibitions/{id}/like, DELETE /exhibitions/{id}/like */
export async function toggleExhibitionLike(id: string, liked: boolean): Promise<{ likes: number }> {
  if (USE_MOCK) {
    const project = MOCK_EXHIBITIONS.find((item) => item.id === id);
    const base = project?.likeCount ?? 0;
    return mockResponse({ likes: liked ? base + 1 : base });
  }
  return liked
    ? http.post<{ likes: number }>(`/exhibitions/${id}/like`)
    : http.delete<{ likes: number }>(`/exhibitions/${id}/like`);
}

/** POST /exhibitions/{id}/bookmark */
export async function toggleExhibitionBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return bookmarked
    ? http.post<void>(`/exhibitions/${id}/bookmark`)
    : http.delete<void>(`/exhibitions/${id}/bookmark`);
}

/**
 * POST /exhibitions/{id}/comments
 * parentId 를 주면 그 원댓글의 답글이 된다 — 답글의 답글은 허용하지 않는다 (기획서 3.8).
 */
export async function createExhibitionComment(
  id: string,
  payload: { content: string; parentId?: string },
): Promise<ExhibitionComment> {
  if (USE_MOCK) {
    const me = MOCK_PROFILES.haneul;
    return mockResponse({
      id: `cm-${Date.now()}`,
      authorName: me.name,
      authorInitial: me.initial,
      content: payload.content,
      createdAt: '방금 전',
      replies: [],
    });
  }
  return http.post<ExhibitionComment>(`/exhibitions/${id}/comments`, payload);
}

/** PUT /exhibitions/{id}/comments/{commentId} */
export async function updateExhibitionComment(
  id: string,
  commentId: string,
  content: string,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.put<void>(`/exhibitions/${id}/comments/${commentId}`, { content });
}

/** DELETE /exhibitions/{id}/comments/{commentId} */
export async function deleteExhibitionComment(id: string, commentId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.delete<void>(`/exhibitions/${id}/comments/${commentId}`);
}

export interface ExhibitionFormPayload {
  source: '파티 연동' | '자기신고';
  partyId?: string;
  title: string;
  coverFileName?: string;
  summary: string;
  description: string;
  category: string;
  link?: string;
  skills: string[];
}

/**
 * DELETE /exhibitions/{id} — 전시를 올린 사람만 지울 수 있다 (파티와 같은 조건).
 * 권한 판단은 서버가 최종적으로 하고, 화면은 버튼을 감추는 것까지만 한다.
 */
export async function deleteExhibition(id: string): Promise<void> {
  if (USE_MOCK) {
    const index = MOCK_EXHIBITIONS.findIndex((project) => project.id === id);
    if (index >= 0) MOCK_EXHIBITIONS.splice(index, 1);
    delete MOCK_EXHIBITION_DETAILS[id];
    return mockResponse(undefined as void);
  }
  return http.delete<void>(`/exhibitions/${id}`);
}

/** POST /exhibitions */
export async function createExhibition(
  payload: ExhibitionFormPayload,
): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id: `exh-${Date.now()}` });
  return http.post<{ id: string }>('/exhibitions', payload);
}

/** PUT /exhibitions/{id} */
export async function updateExhibition(
  id: string,
  payload: ExhibitionFormPayload,
): Promise<{ id: string }> {
  if (USE_MOCK) return mockResponse({ id });
  return http.put<{ id: string }>(`/exhibitions/${id}`, payload);
}
