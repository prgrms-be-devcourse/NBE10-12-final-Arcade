import type { AuthUser, LoginPayload, SignupPayload } from '@/lib/types';
import { MOCK_PROFILES } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

export type MemberType = '일반' | '주최측';

/** POST /auth/login */
export async function login(
  payload: LoginPayload & { memberType: MemberType },
): Promise<AuthUser> {
  if (USE_MOCK) {
    const profile = MOCK_PROFILES.haneul;
    return mockResponse({
      id: profile.id,
      name: profile.name,
      email: payload.email,
      initial: profile.initial,
      role: payload.memberType === '주최측' ? ('HOST' as const) : ('MEMBER' as const),
    });
  }
  return http.post<AuthUser>('/auth/login', payload);
}

/** POST /auth/social/{provider} */
export async function socialLogin(provider: 'github'): Promise<AuthUser> {
  if (USE_MOCK) {
    const profile = MOCK_PROFILES.haneul;
    return mockResponse({
      id: profile.id,
      name: profile.name,
      email: `${provider}@crewon.dev`,
      initial: profile.initial,
      role: 'MEMBER' as const,
    });
  }
  return http.post<AuthUser>(`/auth/social/${provider}`);
}

/** POST /auth/signup */
export async function signup(
  payload: SignupPayload & { memberType: MemberType },
): Promise<AuthUser> {
  if (USE_MOCK) {
    return mockResponse({
      id: `user-${Date.now()}`,
      name: payload.nickname,
      email: payload.email,
      initial: payload.nickname.charAt(0) || 'C',
      role: payload.memberType === '주최측' ? ('HOST' as const) : ('MEMBER' as const),
    });
  }
  return http.post<AuthUser>('/auth/signup', payload);
}

/**
 * POST /me/password — 비밀번호 변경.
 * 이메일 인증(requestEmailVerification → confirmEmailVerification)을 통과한 뒤에만 호출한다.
 */
export async function changePassword(payload: {
  verificationCode: string;
  newPassword: string;
}): Promise<{ changed: boolean; message: string }> {
  if (USE_MOCK) {
    const valid = payload.newPassword.length >= 8;
    return mockResponse({
      changed: valid,
      message: valid
        ? '비밀번호를 변경했어요. 다음 로그인부터 새 비밀번호를 사용해 주세요.'
        : '비밀번호는 8자 이상이어야 해요.',
    });
  }
  return http.post('/me/password', payload);
}

/** POST /auth/logout */
export async function logout(): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>('/auth/logout');
}

/**
 * POST /auth/email/verify-request — 이메일 인증번호 발송
 * 데모에서는 언제나 성공 처리하며, 인증번호는 `000000` 이다.
 */
export async function requestEmailVerification(email: string): Promise<{ sent: boolean }> {
  if (USE_MOCK) return mockResponse({ sent: Boolean(email) });
  return http.post<{ sent: boolean }>('/auth/email/verify-request', { email });
}

/** POST /auth/email/verify-confirm */
export async function confirmEmailVerification(
  email: string,
  code: string,
): Promise<{ verified: boolean; message: string }> {
  if (USE_MOCK) {
    const verified = code.length === 6;
    return mockResponse({
      verified,
      message: verified ? '이메일 인증이 완료됐어요.' : '인증번호 6자리를 입력해 주세요.',
    });
  }
  return http.post('/auth/email/verify-confirm', { email, code });
}
