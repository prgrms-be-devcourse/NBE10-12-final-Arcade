import type {
  BookmarkItem,
  CareerItem,
  MemberRole,
  PositionType,
  ProfileLink,
  TargetType,
  UserProfile,
} from '@/lib/types';
import { toPositionType } from '@/lib/constants';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES, MOCK_USER_SUMMARIES } from '@/lib/mock';
import { CONTEST_FORMAT_LABELS, GOAL_SOURCE_LABELS, positionLabel } from '@/lib/constants';
import { ApiError, USE_MOCK, http, mockResponse } from './client';
import { toDateText } from './time';
import { type ContestResponse, toContest } from './contests';
import { type GoalResponse, toAchievement } from './goals';
import { type ShowcaseGoalResponse, toExhibitionProject } from './exhibitions';
import { type PartyListItemResponse, toParty } from './parties';

function fallbackProfile(id: string): UserProfile {
  const base = MOCK_PROFILES.haneul;
  const summary = MOCK_USER_SUMMARIES[id];
  if (!summary) return base;
  return { ...base, ...summary };
}

/** 백엔드 MemberProfileDto — GET/PATCH /api/v1/members/me 응답 */
export interface MemberProfileResponse {
  id: number;
  email: string;
  /** 소셜 로그인으로 생성된 회원은 이름을 아직 설정하지 않아 null일 수 있다. */
  name: string | null;
  nickname: string | null;
  webpage: string | null;
  profileImageUrl: string | null;
  /** GitHub 이 준 아바타. 서버가 profileImageUrl 과 합치지 않고 따로 내려준다 */
  githubAvatarUrl: string | null;
  githubLinked: boolean;
  bio: string | null;
  /** 대표 포지션 하나. 고르지 않았으면 null */
  position: PositionType | null;
  techStacks: string[];
  careers: MemberCareerResponse[];
  links: MemberLinkResponse[];
}

export interface MemberCareerResponse {
  id: number;
  /** yyyy-MM-dd. 없을 수 있다 */
  startDate: string | null;
  /** null 이면 재직중 */
  endDate: string | null;
  role: string | null;
  org: string | null;
  description: string | null;
}

export interface MemberLinkResponse {
  id: number;
  label: string;
  url: string;
}

/** 시작·종료일로 화면에 보여줄 기간 문구를 만든다. 종료일이 없으면 재직중이다. */
function toPeriod(startDate: string | null, endDate: string | null): string {
  const format = (value: string) => value.slice(0, 7).replace('-', '.');
  if (!startDate) return endDate ? `~ ${format(endDate)}` : '';
  return `${format(startDate)} ~ ${endDate ? format(endDate) : '재직중'}`;
}

/**
 * 백엔드 MemberProfileDto 를 화면 UserProfile 로 옮긴다.
 *
 * 프로필 응답에 없어서 여기서 채우지 않는 값:
 * - stats, streakDays, activityHeatmap, badges : GET /members/me/summary 가 따로 준다(fetchMySummary).
 *   집계 쿼리가 무거워 서버가 일부러 나눠 뒀다 - 세션 확인용 GET /me 마다 돌면 안 된다
 * - achievements  : GET /goals/me 로 따로 읽어 마이페이지에서 합친다
 * - memberRole    : 응답에 role 이 없다. 로그인 응답에서 받아 덮어쓴다
 */
export function toUserProfile(
  dto: MemberProfileResponse,
  memberRole: MemberRole = 'MEMBER',
): UserProfile {
  // 소셜 로그인 신규 회원은 nickname과 name이 모두 비어 있을 수 있다.
  // 로그인 판별 과정에서 예외가 나지 않도록 표시명 기본값을 둔다.
  const displayName = dto.nickname?.trim() || dto.name?.trim() || '크루온 사용자';

  return {
    id: String(dto.id),
    name: displayName,
    // 표시명(displayName)과 달리 대체 문구를 넣지 않는다 - 수정 폼이 이 값을 초기값으로 쓴다
    nickname: dto.nickname?.trim() || undefined,
    initial: displayName.charAt(0) || 'C',
    // 서버가 둘을 합치지 않고 그대로 내려주므로 화면이 고른다
    avatarUrl: dto.profileImageUrl ?? dto.githubAvatarUrl ?? undefined,
    uploadedImageUrl: dto.profileImageUrl ?? undefined,
    // UserSummary.role 은 계정 권한이 아니라 화면에 보여주는 대표 포지션 문구다
    role: dto.position ? positionLabel(dto.position) : '',
    memberRole,
    githubLinked: dto.githubLinked,
    bio: dto.bio ?? '',
    // 고르지 않았으면 null 이 온다. 화면 select 의 기본값을 BACK 으로 둔다
    // (role 은 위에서 따로 본다 - 안 고른 사람에게 '백엔드' 라고 적으면 안 되기 때문이다)
    position: toPositionType(dto.position),
    skills: dto.techStacks,
    stats: { completedParties: 0, awards: 0, exhibitions: 0, approvalRate: 0 },
    streakDays: 0,
    badges: [],
    achievements: [],
    careers: dto.careers.map((career) => ({
      id: String(career.id),
      period: toPeriod(career.startDate, career.endDate),
      title: career.role ?? '',
      org: career.org ?? '',
      description: career.description ?? '',
      startDate: career.startDate ?? undefined,
      endDate: career.endDate ?? undefined,
    })),
    links: dto.links.map((link) => ({
      id: String(link.id),
      label: link.label,
      url: link.url,
    })),
  };
}

/** GET /api/v1/members/me */
export async function fetchMyProfile(): Promise<UserProfile> {
  if (USE_MOCK) return mockResponse(MOCK_PROFILES[MOCK_CURRENT_USER_ID]);
  return toUserProfile(await http.get<MemberProfileResponse>('/members/me'));
}

/**
 * 로그인하지 않았으면 null 을 돌려주는 버전.
 *
 * 서버 컴포넌트에서 프로필을 읽을 때 쓴다. 비로그인 401 을 그대로 던지면
 * 페이지 전체가 500 으로 죽어 로그인 화면조차 볼 수 없게 된다.
 *
 * 잡는 건 401 뿐이다. 5xx 는 서버 장애지 '로그인 안 됨' 이 아니라서 그대로 올려보낸다 —
 * 삼키면 로그인한 사람에게 비로그인 화면이 조용히 뜨고 원인이 드러나지 않는다.
 */
export async function fetchMyProfileOrNull(): Promise<UserProfile | null> {
  try {
    return await fetchMyProfile();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

/** 백엔드 MemberPublicProfileDto — GET /api/v1/members/{id} 응답 */
export interface MemberPublicProfileResponse
  extends Omit<MemberProfileResponse, 'email' | 'githubLinked'> {
  completedParties: number;
  awards: number;
  exhibitions: number;
  /** 연속 활동일 */
  streakDays: number;
  /** 최근 8주(56일) 활동 농도 0~3 */
  activityHeatmap: number[];
  /** 달성한 CONTEST 성취 */
  achievements: GoalResponse[];
  /** 가입 시각(ISO). '크루온 활동 N개월째' 를 여기서 센다 */
  joinedAt: string;
}

/**
 * GET /api/v1/members/{id} — 특정 회원의 공개 프로필.
 *
 * 성취는 별도 비공개 설정 없이 전체 공개라(기획서 2.5) 파티장이 지원자 이력을 여기서 확인한다.
 *
 * 내 프로필과 달리 집계·성취가 한 응답에 함께 온다 - 남의 프로필을 열 때만 부르는 경로라
 * /me 처럼 나눌 이유가 없다. email·githubLinked 는 본인 화면 전용이라 응답에 없다.
 *
 * 로그인 없이 열린다 - 조회는 비인증이 기획서 9.x 의 규칙이라 파티·전시 조회와 같은 취급이다.
 * 공개해선 안 되는 값은 인가가 아니라 응답에서 빠져 있다(email·githubLinked).
 *
 * 없는 회원이면 404, 인가 규칙이 바뀌면 401 이 온다. 둘 다 '볼 수 없음' 이라 null 이다.
 */
export async function fetchUserProfile(id: string): Promise<UserProfile | null> {
  if (USE_MOCK) return mockResponse(MOCK_PROFILES[id] ?? fallbackProfile(id));

  try {
    const dto = await http.get<MemberPublicProfileResponse>(`/members/${id}`);

    return {
      // 공개 응답에 없는 두 값만 채워 내 프로필과 같은 변환을 그대로 쓴다
      ...toUserProfile({ ...dto, email: '', githubLinked: false }),
      stats: {
        completedParties: dto.completedParties,
        awards: dto.awards,
        exhibitions: dto.exhibitions,
        // 승인/거절 이력을 집계하는 값이 서버에 아직 없다(내 요약도 마찬가지다)
        approvalRate: 0,
      },
      streakDays: dto.streakDays,
      activityHeatmap: dto.activityHeatmap,
      achievements: dto.achievements.map(toAchievement),
      joinedAt: dto.joinedAt,
    };
  } catch (error) {
    if (error instanceof ApiError && (error.status === 404 || error.status === 401)) return null;
    throw error;
  }
}

/**
 * POST /api/v1/members/me/image — 프로필 이미지 업로드.
 *
 * 저장된 이미지의 URL 을 돌려준다. 이 값을 updateMyProfile 의 profileImageUrl 로 실어야
 * 실제로 프로필에 붙는다 — 업로드만 하면 파일만 올라가고 프로필은 그대로다.
 *
 * jpg·png 5MB 까지. 초과하거나 형식이 다르면 서버가 400-1 과 함께 사유를 msg 로 준다.
 */
export async function uploadProfileImage(file: File): Promise<string> {
  if (USE_MOCK) return mockResponse(URL.createObjectURL(file));

  const form = new FormData();
  form.append('file', file);

  const { profileImageUrl } = await http.post<{ profileImageUrl: string }>(
    '/members/me/image',
    form,
  );
  return profileImageUrl;
}

export interface ProfileUpdatePayload {
  position: PositionType;
  bio: string;
  /**
   * uploadProfileImage() 가 돌려준 URL.
   * 생략(undefined)하면 서버가 지금 값을 그대로 둔다. 지우려면 null 을 준다(빈 문자열로 보낸다).
   */
  profileImageUrl?: string | null;
  /** undefined 면 닉네임을 그대로 둔다. 서버가 빈 문자열을 거절하므로(400-1) 비울 수는 없다 */
  nickname?: string;
  skills: string[];
  careers: CareerItem[];
  links: ProfileLink[];
}

/**
 * PATCH /api/v1/members/me
 *
 * **보낸 항목만 바뀐다**(ARC-120). 생략하면 서버가 그 항목을 그대로 두고,
 * 비우려면 빈 값을 명시해야 한다 - 문자열은 '', 목록은 []. null 은 '건드리지 말라'는 뜻이다.
 * 그래서 화면에 칸이 없는 webpage 는 아예 싣지 않는다(예전엔 null 을 실어 저장할 때마다 지워졌다).
 *
 * careers 는 role 이, links 는 label·url 이 비어 있으면 서버가 그 항목을 버린다.
 * 성취는 이 화면에서 다루지 않는다 - 등록·수정은 /goals 화면이 담당한다.
 */
export async function updateMyProfile(payload: ProfileUpdatePayload): Promise<UserProfile> {
  if (USE_MOCK) {
    const current = MOCK_PROFILES[MOCK_CURRENT_USER_ID];
    return mockResponse({
      ...current,
      name: payload.nickname ?? current.name,
      nickname: payload.nickname ?? current.nickname,
      initial: (payload.nickname ?? current.name).charAt(0),
      position: payload.position,
      bio: payload.bio,
      skills: payload.skills,
      achievements: current.achievements,
      careers: payload.careers,
      links: payload.links,
    });
  }

  const updated = await http.patch<MemberProfileResponse>('/members/me', {
    bio: payload.bio,
    position: payload.position,
    techStacks: payload.skills,
    careers: payload.careers.map((career) => ({
      startDate: career.startDate || null,
      endDate: career.endDate || null,
      role: career.title,
      org: career.org,
      description: career.description,
    })),
    links: payload.links.map((link) => ({ label: link.label, url: link.url })),
    // 닉네임은 비울 수 없다(@Pattern). 안 바꿨으면 키를 빼서 서버가 그대로 두게 한다
    ...(payload.nickname === undefined ? {} : { nickname: payload.nickname }),
    // undefined 면 키 자체가 빠져 서버가 지금 이미지를 유지한다. null 은 '지워 달라'라서 '' 로 보낸다
    ...(payload.profileImageUrl === undefined
      ? {}
      : { profileImageUrl: payload.profileImageUrl ?? '' }),
  });
  return toUserProfile(updated);
}

/** 백엔드 MyBookmarkDto — 대상 종류에 따라 target 의 모양이 다르다 */
interface MyBookmarkResponse {
  id: number;
  targetType: TargetType;
  target: PartyListItemResponse | ContestResponse | ShowcaseGoalResponse;
  bookmarkedAt: string;
}

/**
 * 북마크 한 건을 카드 한 줄로 눌러 담는다.
 *
 * target 은 파티 목록·대회 허브·전시관이 쓰는 카드 DTO 그대로라,
 * 각 모듈의 매퍼를 재사용해 화면 도메인 객체로 옮긴 뒤 공통 필드만 뽑는다.
 */
function toBookmarkItem(dto: MyBookmarkResponse): BookmarkItem {
  const savedAt = toDateText(dto.bookmarkedAt);

  if (dto.targetType === 'PARTY') {
    const party = toParty(dto.target as PartyListItemResponse);
    const filled = party.positions.reduce((sum, position) => sum + position.filledCount, 0);
    const capacity = party.positions.reduce((sum, position) => sum + position.capacity, 0);
    return {
      id: String(dto.id),
      targetType: 'PARTY',
      targetId: party.id,
      title: party.title,
      subtitle: `${party.leader.name} · ${filled}/${capacity}명`,
      meta: party.dday,
      tags: party.subCategory ? [party.subCategory] : [],
      createdAt: savedAt,
    };
  }

  if (dto.targetType === 'CONTEST') {
    const contest = toContest(dto.target as ContestResponse);
    return {
      id: String(dto.id),
      targetType: 'CONTEST',
      targetId: contest.id,
      title: contest.title,
      subtitle: `접수 ${contest.period}`,
      meta: contest.dday,
      tags: [CONTEST_FORMAT_LABELS[contest.format], contest.tag],
      createdAt: savedAt,
    };
  }

  const project = toExhibitionProject(dto.target as ShowcaseGoalResponse);
  return {
    id: String(dto.id),
    targetType: 'GOAL',
    targetId: project.id,
    title: project.title,
    subtitle: project.partyName || '개인 성취',
    meta: `좋아요 ${project.likeCount}`,
    tags: [project.category, GOAL_SOURCE_LABELS[project.source]],
    createdAt: savedAt,
  };
}

/** 백엔드 MemberSummaryDto — GET /api/v1/members/me/summary 응답 */
export interface MemberSummaryResponse {
  completedParties: number;
  awards: number;
  exhibitions: number;
  streakDays: number;
  /** 최근 8주(56일) 활동 농도를 오래된 날부터 늘어놓은 0~3 값 */
  activityHeatmap: number[];
  /** 배지 도메인이 없어 아직 빈 배열이다 */
  badges: string[];
  /** 가입 시각(ISO). '크루온 활동 N개월째' 를 여기서 센다 */
  joinedAt: string;
}

/** UserProfile 에서 요약 API 가 채우는 부분 */
export type ProfileSummary = Pick<
  UserProfile,
  'stats' | 'streakDays' | 'activityHeatmap' | 'badges' | 'joinedAt'
>;

/**
 * GET /api/v1/members/me/summary — 마이페이지 '활동 스코어' 카드 값.
 *
 * 프로필(GET /me)과 나뉘어 있다. 그쪽은 세션 확인용이라 거의 모든 화면이 부르는데
 * 집계 쿼리를 섞으면 닉네임 한 줄 고칠 때마다 함께 돈다.
 *
 * approvalRate 는 서버에 없다 - 승인/거절 이력을 집계하는 값이 아직 없어 0 으로 둔다.
 */
export async function fetchMySummary(): Promise<ProfileSummary> {
  if (USE_MOCK) {
    const mock = MOCK_PROFILES[MOCK_CURRENT_USER_ID];
    return mockResponse({
      stats: mock.stats,
      streakDays: mock.streakDays,
      activityHeatmap: mock.activityHeatmap,
      badges: mock.badges,
      joinedAt: mock.joinedAt,
    });
  }

  const dto = await http.get<MemberSummaryResponse>('/members/me/summary');
  return {
    stats: {
      completedParties: dto.completedParties,
      awards: dto.awards,
      exhibitions: dto.exhibitions,
      approvalRate: 0,
    },
    streakDays: dto.streakDays,
    activityHeatmap: dto.activityHeatmap,
    // 서버가 배지 이름만 준다. 도메인이 생기기 전까지는 아이콘·획득 여부를 알 수 없다
    badges: dto.badges.map((label) => ({ id: label, label, icon: 'star', earned: true })),
    joinedAt: dto.joinedAt,
  };
}

/**
 * 로그인하지 않았으면 비어 있는 요약을 돌려주는 버전.
 * 마이페이지는 서버 컴포넌트에서 프로필과 함께 읽는데, 401 을 그대로 던지면 화면 전체가 죽는다.
 */
export async function fetchMySummaryOrEmpty(): Promise<ProfileSummary> {
  try {
    return await fetchMySummary();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return {
        stats: { completedParties: 0, awards: 0, exhibitions: 0, approvalRate: 0 },
        streakDays: 0,
        activityHeatmap: [],
        badges: [],
      };
    }
    throw error;
  }
}

/**
 * GET /api/v1/members/me/bookmarks — 파티·대회·전시를 한 목록에 섞어 최근 담은 순으로.
 *
 * target 은 targetType 에 따라 모양이 다르다 - 각 목록 API 가 쓰는 카드 DTO 를 그대로 싣는다.
 * 여기서 북마크함 카드 한 줄(title·subtitle·meta·tags)로 눌러 담는다.
 *
 * 대상이 삭제됐거나 전시가 내려간 북마크는 서버가 목록에서 빼므로 그 페이지만 size 보다 짧을 수 있다.
 */
export async function fetchMyBookmarks(): Promise<BookmarkItem[]> {
  if (USE_MOCK) {
    const { MOCK_BOOKMARKS } = await import('@/lib/mock');
    return mockResponse(MOCK_BOOKMARKS);
  }

  const page = await http.get<{ content: MyBookmarkResponse[] }>('/members/me/bookmarks', {
    query: { size: 60 },
  });
  return page.content.map(toBookmarkItem);
}
