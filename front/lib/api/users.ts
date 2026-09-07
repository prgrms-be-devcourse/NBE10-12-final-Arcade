import type {
  BookmarkItem,
  CareerItem,
  MemberRole,
  PositionType,
  ProfileLink,
  UserProfile,
} from '@/lib/types';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES, MOCK_USER_SUMMARIES } from '@/lib/mock';
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
  /** 서버는 BACK/FRONT/UIUX/PM 4종을 모두 내려줄 수 있다 */
  positions: string[];
  techStacks: string[];
}

/**
 * 서버가 주는 포지션 문자열을 화면 타입으로 좁힌다.
 * 화면 PositionType 은 이번 스코프에서 BACK/FRONT 만 쓰기로 해(lib/types.ts) UIUX·PM 이 빠져 있다.
 */
function toPositionType(value: string | undefined): PositionType {
  return value === 'FRONT' ? 'FRONT' : 'BACK';
}

/**
 * 백엔드 MemberProfileDto 를 화면 UserProfile 로 옮긴다.
 *
 * 백엔드에 아직 없어서 비워 두는 값:
 * - bio, careers, links      : 프로필 확장 필드 미구현
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
    role: dto.positions[0] ?? '',
    memberRole,
    githubLinked: dto.githubLinked,
    githubUsername: undefined,
    bio: '',
    position: toPositionType(dto.positions[0]),
    skills: dto.techStacks,
    stats: { completedParties: 0, awards: 0, exhibitions: 0, approvalRate: 0 },
    streakDays: 0,
    badges: [],
    achievements: [],
    careers: [],
    links: [],
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
 * 서버가 받는 값은 nickname·webpage·profileImageUrl·positions·techStacks 뿐이다.
 * bio·githubUsername·careers·links 는 저장되지 않는다 (백엔드 필드 미구현).
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
    positions: [payload.position],
    techStacks: payload.skills,
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
