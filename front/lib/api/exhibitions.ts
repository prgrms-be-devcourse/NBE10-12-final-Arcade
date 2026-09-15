import type {
  ExhibitionDetail,
  ExhibitionProject,
  GoalSource,
  GoalStatus,
  GoalType,
  PositionType,
  ThreadComment,
} from '@/lib/types';
import { timeAgo } from './time';
import {
  MOCK_EXHIBITIONS,
  MOCK_EXHIBITION_DETAILS,
  MOCK_EXHIBITION_COMMITS,
  MOCK_PROFILES,
} from '@/lib/mock';
import { GOAL_TYPE_LABELS } from '@/lib/constants';
import { USE_MOCK as USE_API_MOCK, http, mockResponse } from './client';
import { toDateText } from './time';

/**
 * 이 모듈에서 **아직 서버가 없는 기능**만 데모 데이터로 고정하는 스위치다.
 *
 * 목록·상세·게시·좋아요·북마크는 서버가 있어서 client 의 USE_MOCK(USE_API_MOCK)을 쓴다.
 * 남은 것은 커밋 스냅샷·전시 직접 등록/수정/삭제로, 대응 엔드포인트가 없다.
 * (댓글은 ARC-160 으로 서버가 생겨 USE_API_MOCK 으로 옮겼다)
 *
 * 없는 경로로 요청하면 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다. 그래서 이 상수로 막아 둔다.
 *
 * 서버가 생기면 해당 함수의 USE_MOCK 을 USE_API_MOCK 으로 바꾸면 된다.
 */
const USE_MOCK: boolean = true;


/** LikeDto — 좋아요 응답 */
interface LikeResponse {
  targetType: 'PARTY' | 'CONTEST' | 'GOAL' | 'PARTY_SHOWCASE';
  targetId: number;
  liked: boolean;
  likeCount: number;
}

/**
 * 서버 ShowcaseGoalDto — 전시관 목록의 한 칸.
 * step1 이후 대상은 "게시된 파티 결과물(PROJECT)"뿐이라 type=PROJECT / status=ACHIEVED /
 * source=PLATFORM_VERIFIED 로 고정되고, party·positionType 이 항상 채워진다.
 */
export interface ShowcaseGoalResponse {
  id: number;
  party: { id: number; name: string };
  type: GoalType;
  status: GoalStatus;
  source: GoalSource;
  detail: { title: string | null };
  positionType: PositionType;
  likeCount: number;
  /** 원본 파티에 합산된 조회수를 쓴다(기획서 3.2). */
  viewCount: number;
  createAt: string;
}

/**
 * 서버 전시 목록에 없어서 비워 두는 값:
 * - summary, skills, coverImageUrl : ShowcaseGoalDto 에 본문·기술스택·이미지가 없다
 * - leader                         : 소유자 정보가 아예 없어 카드에서 생략된다
 * - likedByMe, bookmarkedByMe      : 응답에 없다 (docs/마이페이지-요약API_백엔드_요청.md ⑤)
 */
export function toExhibitionProject(dto: ShowcaseGoalResponse): ExhibitionProject {
  return {
    id: String(dto.id),
    title: dto.detail.title ?? '',
    summary: '',
    partyName: dto.party.name,
    role: dto.positionType,
    category: GOAL_TYPE_LABELS[dto.type],
    source: dto.source,
    skills: [],
    viewCount: dto.viewCount,
    likeCount: dto.likeCount,
    sourcePartyId: String(dto.party.id),
    thumbnailLabel: GOAL_TYPE_LABELS[dto.type],
  };
}

/**
 * GET /api/v1/showcase/goals — 전시 성취 목록 (ARC-96).
 *
 * 파티장이 전시글을 게시해 partyShowcase 가 연결된 PROJECT 성취만 온다.
 * 좋아요/북마크 가능 조건과 같은 기준이다.
 */
export async function fetchExhibitions(
  sort: 'like' | 'recent' = 'like',
): Promise<ExhibitionProject[]> {
  if (USE_API_MOCK) {
    const sorted =
      sort === 'like'
        ? [...MOCK_EXHIBITIONS].sort((a, b) => b.likeCount - a.likeCount)
        : MOCK_EXHIBITIONS;
    return mockResponse(sorted);
  }

  const page = await http.get<{ content: ShowcaseGoalResponse[] }>('/showcase/goals', {
    query: { sort: sort === 'like' ? 'POPULAR' : 'LATEST', size: 60 },
  });
  return page.content.map(toExhibitionProject);
}

/** 서버 PartyShowcaseDto — 파티 전시 초안·게시 응답 */
interface PartyShowcaseResponse {
  partyId: number;
  partyName: string;
  ownerName: string;
  memberNames: string[];
  githubRepoUrl: string | null;
  title: string | null;
  description: string | null;
  published: boolean;
  publishedAt: string | null;
  viewCount: number;
  likeCount: number;
}

/** 이름만 아는 참여자. id 가 없으면 프로필 링크·쪽지 버튼을 걸 수 없다. */
const nameOnlyUser = (name: string) => ({
  id: '',
  name,
  initial: name.charAt(0) || 'C',
  role: '',
});

/**
 * 서버 전시 상세에 없어서 비워 두는 값:
 * - skills, coverImageUrl : PartyShowcaseDto 에 기술스택·이미지가 없다 (요청서 ⑥)
 * - comments              : 댓글 API 가 없다 (요청서 ⑦) - 화면에서도 숨긴다
 * - members[].id          : 참여자는 이름 목록으로만 온다
 */
function toExhibitionDetail(dto: PartyShowcaseResponse): ExhibitionDetail {
  const description = dto.description ?? '';

  return {
    id: String(dto.partyId),
    title: dto.title ?? dto.partyName,
    summary: description,
    partyName: dto.partyName,
    role: 'BACK',
    category: GOAL_TYPE_LABELS.PROJECT,
    // 파티 전시는 크루온 활동으로 만들어진 기록이다
    source: 'PLATFORM_VERIFIED',
    skills: [],
    viewCount: dto.viewCount,
    likeCount: dto.likeCount,
    sourcePartyId: String(dto.partyId),
    leader: nameOnlyUser(dto.ownerName),
    thumbnailLabel: GOAL_TYPE_LABELS.PROJECT,
    description,
    members: dto.memberNames.map(nameOnlyUser),
    links: dto.githubRepoUrl ? [{ id: 'github', label: 'GitHub', url: dto.githubRepoUrl }] : [],
    period: dto.publishedAt ? `${toDateText(dto.publishedAt)} 게시` : '',
    comments: [],
  };
}

/**
 * GET /api/v1/parties/{partyId}/showcase — **게시된** 파티 전시 상세.
 *
 * 전시는 파티에 종속이라 id 는 goal id 가 아니라 **partyId** 다.
 * 비로그인도 볼 수 있다. 아직 게시하지 않은 파티는 404 이므로,
 * 게시 화면처럼 초안을 읽어야 하는 곳은 fetchExhibitionDraft 를 쓴다 (ARC-160 에서 경로가 갈렸다).
 */
export async function fetchExhibition(id: string): Promise<ExhibitionDetail> {
  if (USE_API_MOCK) {
    return mockResponse(MOCK_EXHIBITION_DETAILS[id] ?? MOCK_EXHIBITION_DETAILS['settlement-api']);
  }
  return toExhibitionDetail(await http.get<PartyShowcaseResponse>(`/parties/${id}/showcase`));
}

/**
 * GET /api/v1/parties/{partyId}/showcase/draft — 게시 전 초안.
 *
 * 파티장·승인된 파티원만 볼 수 있다(403). 게시 여부와 무관하게 내려오므로
 * 게시 화면이 제목·설명을 채워 넣을 때 쓴다.
 */
export async function fetchExhibitionDraft(partyId: string): Promise<ExhibitionDetail> {
  if (USE_API_MOCK) {
    return mockResponse(MOCK_EXHIBITION_DETAILS['settlement-api']);
  }
  return toExhibitionDetail(
    await http.get<PartyShowcaseResponse>(`/parties/${partyId}/showcase/draft`),
  );
}


/**
 * POST /api/v1/parties/{partyId}/showcase — 전시 게시.
 *
 * 파티장만 부를 수 있다(403). 이미 게시된 파티에 다시 부르면 제목·설명이 갱신된다 —
 * 서버가 기존 PartyShowcase 를 찾아 다시 publish 하므로 등록과 수정이 같은 호출이다.
 */
export async function publishPartyShowcase(
  partyId: string,
  payload: { title: string; description: string },
): Promise<ExhibitionDetail> {
  if (USE_API_MOCK) {
    return mockResponse(MOCK_EXHIBITION_DETAILS['settlement-api']);
  }
  return toExhibitionDetail(
    await http.post<PartyShowcaseResponse>(`/parties/${partyId}/showcase`, payload),
  );
}

/**
 * GET /exhibitions/{id}/commits — 완료 시점 커밋 스냅샷.
 * 팀 스페이스의 커밋 내역을 완료 시점 그대로 얼려 보여준다.
 */
export async function fetchExhibitionCommits(id: string) {
  if (USE_MOCK) return mockResponse(MOCK_EXHIBITION_COMMITS[id] ?? []);
  return http.get(`/exhibitions/${id}/commits`);
}

/**
 * 전시 좋아요 — 서버 경로는 성취 기준이다. POST /api/v1/goals/{goalId}/likes, DELETE 로 취소.
 *
 * 전시 카드의 id 가 곧 goal id 다. PROJECT 성취는 서버가 좋아요 행을 파티 전시(PARTY_SHOWCASE)로
 * 라우팅해 성취 카드와 파티에서 누른 좋아요가 같은 행을 쓴다(ARC-117) - 화면은 신경 쓸 게 없다.
 *
 * 취소 응답에는 본문이 없고 성취 상세에도 likeCount 가 없어, 버튼이 이미 반영한 수를 그대로 돌려준다.
 */
export async function toggleExhibitionLike(
  id: string,
  liked: boolean,
  currentCount = 0,
): Promise<{ likes: number }> {
  if (USE_API_MOCK) {
    const project = MOCK_EXHIBITIONS.find((item) => item.id === id);
    const base = project?.likeCount ?? 0;
    return mockResponse({ likes: liked ? base + 1 : base });
  }

  if (!liked) {
    await http.delete<void>(`/goals/${id}/likes`);
    return { likes: currentCount };
  }
  const result = await http.post<LikeResponse>(`/goals/${id}/likes`);
  return { likes: result.likeCount };
}

/** POST /api/v1/goals/{goalId}/bookmarks, DELETE 로 취소 */
export async function toggleExhibitionBookmark(id: string, bookmarked: boolean): Promise<void> {
  if (USE_API_MOCK) return mockResponse(undefined as void);
  if (bookmarked) {
    await http.post<unknown>(`/goals/${id}/bookmarks`);
    return;
  }
  await http.delete<void>(`/goals/${id}/bookmarks`);
}

/** 백엔드 ShowcaseCommentDto — GET /parties/{partyId}/showcase/comments */
interface ShowcaseCommentResponse {
  id: number;
  authorId: number;
  authorName: string;
  isPartyMember: boolean;
  content: string;
  deleted: boolean;
  createDate: string;
  replies: ShowcaseCommentResponse[];
}

/**
 * 지워진 댓글도 그대로 내려온다(soft delete) - 달린 답글을 살리기 위해서다.
 * 화면이 내용 대신 안내를 보여줄 수 있게 deleted 를 그대로 넘긴다.
 */
function toThreadComment(dto: ShowcaseCommentResponse): ThreadComment {
  return {
    id: String(dto.id),
    authorId: String(dto.authorId),
    authorName: dto.authorName,
    authorInitial: dto.authorName?.trim().charAt(0) ?? '?',
    content: dto.content,
    createdAt: timeAgo(dto.createDate),
    deleted: dto.deleted,
    isPartyMember: dto.isPartyMember,
    replies: (dto.replies ?? []).map(toThreadComment),
  };
}

/** GET /api/v1/parties/{partyId}/showcase/comments — 전시 댓글 목록(답글 포함) */
export async function fetchExhibitionComments(partyId: string): Promise<ThreadComment[]> {
  if (USE_API_MOCK) return mockResponse([] as ThreadComment[]);

  const rows = await http.get<ShowcaseCommentResponse[]>(
    `/parties/${partyId}/showcase/comments`,
  );
  return rows.map(toThreadComment);
}

/**
 * POST /api/v1/parties/{partyId}/showcase/comments
 * parentId 를 주면 그 원댓글의 답글이 된다 — 답글의 답글은 서버가 막는다(1단계 제한).
 */
export async function createExhibitionComment(
  partyId: string,
  payload: { content: string; parentId?: string },
): Promise<ThreadComment> {
  if (USE_API_MOCK) {
    const me = MOCK_PROFILES.haneul;
    return mockResponse({
      id: `cm-${Date.now()}`,
      authorId: 'me',
      authorName: me.name,
      authorInitial: me.initial,
      content: payload.content,
      createdAt: '방금 전',
      deleted: false,
      isPartyMember: false,
      replies: [],
    });
  }
  return toThreadComment(
    await http.post<ShowcaseCommentResponse>(`/parties/${partyId}/showcase/comments`, {
      content: payload.content,
      parentId: payload.parentId ? Number(payload.parentId) : null,
    }),
  );
}

/** PUT /api/v1/parties/{partyId}/showcase/comments/{commentId} — 작성자와 관리자만 */
export async function updateExhibitionComment(
  partyId: string,
  commentId: string,
  content: string,
): Promise<void> {
  if (USE_API_MOCK) return mockResponse(undefined as void);
  return http.put<void>(`/parties/${partyId}/showcase/comments/${commentId}`, { content });
}

/** DELETE /api/v1/parties/{partyId}/showcase/comments/{commentId} — soft delete */
export async function deleteExhibitionComment(
  partyId: string,
  commentId: string,
): Promise<void> {
  if (USE_API_MOCK) return mockResponse(undefined as void);
  return http.delete<void>(`/parties/${partyId}/showcase/comments/${commentId}`);
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
