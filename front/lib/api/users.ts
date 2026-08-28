import type {
  Achievement,
  BookmarkItem,
  CareerItem,
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

/** GET /me */
export async function fetchMyProfile(): Promise<UserProfile> {
  if (USE_MOCK) return mockResponse(MOCK_PROFILES[MOCK_CURRENT_USER_ID]);
  return http.get<UserProfile>('/me');
}

/** GET /users/{id} */
export async function fetchUserProfile(id: string): Promise<UserProfile> {
  if (USE_MOCK) return mockResponse(MOCK_PROFILES[id] ?? fallbackProfile(id));
  return http.get<UserProfile>(`/users/${id}`);
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

/** PUT /me */
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
  return http.put<UserProfile>('/me', payload);
}

/** GET /me/bookmarks — 파티·대회·성취를 대상 구분 없이 한 목록으로 (기획서 2.11) */
export async function fetchMyBookmarks(): Promise<BookmarkItem[]> {
  if (USE_MOCK) {
    const { MOCK_BOOKMARKS } = await import('@/lib/mock');
    return mockResponse(MOCK_BOOKMARKS);
  }
  return http.get<BookmarkItem[]>('/me/bookmarks');
}
