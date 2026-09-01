import type {
  Achievement,
  BookmarkItem,
  CareerItem,
  MemberRole,
  PositionType,
  ProfileLink,
  UserProfile,
} from '@/lib/types';
import { MOCK_CURRENT_USER_ID, MOCK_PROFILES, MOCK_USER_SUMMARIES } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

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
  name: string;
  nickname: string | null;
  webpage: string | null;
  profileImageUrl: string | null;
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
 * - achievements             : GET /goals/me 미구현 (성취 등록만 붙어 있음)
 * - memberRole               : GET /members/me 응답에 role 이 없다. 로그인 응답에서 받아 덮어쓴다.
 */
export function toUserProfile(
  dto: MemberProfileResponse,
  memberRole: MemberRole = 'MEMBER',
): UserProfile {
  const displayName = dto.nickname ?? dto.name;

  return {
    id: String(dto.id),
    name: displayName,
    initial: displayName.charAt(0) || 'C',
    avatarUrl: dto.profileImageUrl ?? undefined,
    // UserSummary.role 은 계정 권한이 아니라 화면에 보여주는 대표 포지션 문구다
    role: dto.positions[0] ?? '',
    memberRole,
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
 * 특정 회원의 공개 프로필.
 * 백엔드에 GET /members/{id} 가 아직 없어 데모 데이터를 그대로 쓴다.
 */
export async function fetchUserProfile(id: string): Promise<UserProfile> {
  return mockResponse(MOCK_PROFILES[id] ?? fallbackProfile(id));
}

export interface ProfileUpdatePayload {
  nickname: string;
  position: PositionType;
  bio: string;
  /** GitHub 사용자명 — 팀 커밋 작성자와 회원을 연결하는 데 쓴다 */
  githubUsername?: string;
  avatarFileName?: string;
  skills: string[];
  achievements: Achievement[];
  careers: CareerItem[];
  links: ProfileLink[];
}

/**
 * PATCH /api/v1/members/me
 *
 * 서버가 받는 값은 nickname·webpage·profileImageUrl·positions·techStacks 뿐이다.
 * bio·githubUsername·achievements·careers·links 는 저장되지 않는다 (백엔드 필드 미구현).
 * 특히 성취는 이 화면에서 편집해도 반영되지 않으니, POST /goals 로 등록하는 흐름을 따로 붙여야 한다.
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
      achievements: payload.achievements,
      careers: payload.careers,
      links: payload.links,
    });
  }

  const updated = await http.patch<MemberProfileResponse>('/members/me', {
    nickname: payload.nickname,
    webpage: null,
    profileImageUrl: null,
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
