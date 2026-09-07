import type {
  BookmarkItem,
  CareerItem,
  MemberRole,
  PositionType,
  ProfileLink,
  UserProfile,
} from '@/lib/types';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES, MOCK_USER_SUMMARIES } from '@/lib/mock';
import { positionLabel } from '@/lib/constants';
import { ApiError, USE_MOCK, http, mockResponse } from './client';

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
  /** 직접 적은 GitHub 사용자명. OAuth 연동 여부는 githubLinked 로 따로 본다 */
  githubUsername: string | null;
  /** 대표 포지션 하나. 고르지 않았으면 null */
  position: string | null;
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

/**
 * 서버가 주는 포지션 문자열을 화면 타입으로 좁힌다.
 * 화면 PositionType 은 BACK/FRONT 만 쓰기로 해(lib/types.ts) 서버의 UIUX·PM 은 BACK 으로 떨어진다.
 */
function toPositionType(value: string | null | undefined): PositionType {
  return value === 'FRONT' ? 'FRONT' : 'BACK';
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
 * 백엔드에 아직 없어서 비워 두는 값:
 * - stats, streakDays, badges: 마이페이지 요약 API(기획서 9.11) 미구현
 * - achievements             : 프로필 응답에 없다. GET /goals/me 로 따로 읽어 마이페이지에서 합친다
 * - memberRole               : GET /members/me 응답에 role 이 없다. 로그인 응답에서 받아 덮어쓴다.
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
    initial: displayName.charAt(0) || 'C',
    // 서버가 둘을 합치지 않고 그대로 내려주므로 화면이 고른다
    avatarUrl: dto.profileImageUrl ?? dto.githubAvatarUrl ?? undefined,
    uploadedImageUrl: dto.profileImageUrl ?? undefined,
    // UserSummary.role 은 계정 권한이 아니라 화면에 보여주는 대표 포지션 문구다
    role: dto.position ? positionLabel(toPositionType(dto.position)) : '',
    memberRole,
    githubLinked: dto.githubLinked,
    githubUsername: dto.githubUsername ?? undefined,
    bio: dto.bio ?? '',
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

/**
 * 특정 회원의 공개 프로필.
 * 백엔드에 GET /members/{id} 가 아직 없어 데모 데이터를 그대로 쓴다.
 */
export async function fetchUserProfile(id: string): Promise<UserProfile> {
  return mockResponse(MOCK_PROFILES[id] ?? fallbackProfile(id));
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
  nickname: string;
  position: PositionType;
  bio: string;
  /** GitHub 사용자명 — 팀 커밋 작성자와 회원을 연결하는 데 쓴다 */
  githubUsername?: string;
  /** uploadProfileImage() 가 돌려준 URL. 그대로 두려면 기존 값을, 지우려면 null 을 보낸다 */
  profileImageUrl?: string | null;
  skills: string[];
  careers: CareerItem[];
  links: ProfileLink[];
}

/**
 * PATCH /api/v1/members/me
 *
 * 보낸 값이 곧 저장될 값이다 - 생략한 항목은 서버에서 비워진다. 그래서 폼 전체를 항상 실어 보낸다.
 * careers 는 role 이, links 는 label·url 이 비어 있으면 서버가 그 항목을 버린다.
 * 성취는 이 화면에서 다루지 않는다 - 등록·수정은 /goals 화면이 담당한다.
 */
export async function updateMyProfile(payload: ProfileUpdatePayload): Promise<UserProfile> {
  if (USE_MOCK) {
    const current = MOCK_PROFILES[MOCK_CURRENT_USER_ID];
    return mockResponse({
      ...current,
      name: payload.nickname,
      initial: payload.nickname.charAt(0),
      position: payload.position,
      bio: payload.bio,
      githubUsername: payload.githubUsername,
      skills: payload.skills,
      achievements: current.achievements,
      careers: payload.careers,
      links: payload.links,
    });
  }

  const updated = await http.patch<MemberProfileResponse>('/members/me', {
    nickname: payload.nickname,
    webpage: null,
    profileImageUrl: payload.profileImageUrl ?? null,
    bio: payload.bio,
    githubUsername: payload.githubUsername ?? null,
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
  });
  return toUserProfile(updated);
}

/**
 * 내 북마크 목록.
 * 백엔드에 통합 조회 API(기획서 9.11)가 아직 없어 데모 데이터를 그대로 쓴다.
 */
export async function fetchMyBookmarks(): Promise<BookmarkItem[]> {
  const { MOCK_BOOKMARKS } = await import('@/lib/mock');
  return mockResponse(MOCK_BOOKMARKS);
}
