/**
 * 인증 API — ApiV1MemberController 기준.
 *
 * 로그인·재발급이 성공하면 서버가 accessToken·refreshToken 을 쿠키로 심는다.
 * 프론트가 토큰을 따로 보관할 필요가 없고, http 래퍼의 credentials:'include' 만으로 인증이 유지된다.
 */
import type {
  AuthUser,
  LoginPayload,
  MemberRole,
  SignupPayload,
} from "@/lib/types";
import { MOCK_PROFILES } from "@/lib/mock";
import { API_BASE_URL, USE_MOCK, http, mockResponse } from "./client";
import { type MemberProfileResponse } from "./users";

export type MemberType = "일반" | "주최측";

/** 백엔드 MemberLoginDto */
interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  role: MemberRole;
  grantType: string;
  accessTokenExpiresIn: number;
}

/** 백엔드 MemberDto — 회원가입 응답 */
interface SignupResponse {
  id: number;
  email: string;
}

/** 로그인 직후 화면이 쓰는 최소 정보를 /members/me 로 채운다 */
async function loadAuthUser(role: MemberRole): Promise<AuthUser> {
  const profile = await http.get<MemberProfileResponse>("/members/me");
  const displayName = profile.nickname?.trim() || profile.name?.trim() || "크루온 사용자";

  return {
    id: String(profile.id),
    name: displayName,
    email: profile.email,
    initial: displayName.charAt(0) || "C",
    role,
  };
}

/**
 * POST /api/v1/members/login
 *
 * memberType 은 서버로 보내지 않는다. 주최측 전용 로그인 엔드포인트가 따로 없고,
 * 계정 역할(MEMBER/HOST/ADMIN)은 서버가 응답의 role 로 알려준다.
 */
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
      role:
        payload.memberType === "주최측"
          ? ("HOST" as const)
          : ("MEMBER" as const),
    });
  }

  const { email, password } = payload;
  const { role } = await http.post<LoginResponse>("/members/login", {
    email,
    password,
  });
  return loadAuthUser(role);
}

/**
 * GitHub 소셜 로그인.
 *
 * Spring Security 의 OAuth2 리다이렉트 흐름이라 fetch 가 아니라 전체 페이지 이동이다.
 * 서버가 인증을 마치면 쿠키를 심고 redirectUrl 로 되돌려 보낸다.
 * 이동해 버리므로 이 함수는 값을 돌려주지 않는다.
 */
export async function socialLogin(
  provider: "github",
  redirectUrl?: string,
): Promise<void> {
  const targetRedirectUrl = redirectUrl ?? `${window.location.origin}/`;

  if (USE_MOCK) {
    await mockResponse(undefined);
    window.location.assign(targetRedirectUrl);
    return;
  }

  // OAuth2 진입점은 /api/v1 아래가 아니라 서버 루트에 있다
  const origin = API_BASE_URL.replace(/\/api\/v\d+\/?$/, "");
  const target = `${origin}/oauth2/authorization/${provider}?redirectUrl=${encodeURIComponent(targetRedirectUrl)}`;

  window.location.assign(target);
}

/**
 * POST /api/v1/members/signup
 *
 * 가입만으로는 로그인 상태가 되지 않으므로 이어서 로그인하고,
 * 가입 화면에서 고른 포지션은 PATCH /members/me 로 프로필에 저장한다(안 하면 그대로 버려진다).
 *
 * 필수 약관 동의는 **가입 요청에 함께 싣는다** - 가입과 같은 트랜잭션에 기록돼야
 * "가입은 됐는데 동의 기록이 없는" 계정이 생기지 않는다.
 */
export async function signup(
  payload: SignupPayload & { memberType: MemberType },
): Promise<AuthUser> {
  if (USE_MOCK) {
    return mockResponse({
      id: `user-${Date.now()}`,
      name: payload.nickname,
      email: payload.email,
      initial: payload.nickname.charAt(0) || "C",
      role:
        payload.memberType === "주최측"
          ? ("HOST" as const)
          : ("MEMBER" as const),
    });
  }

  const { email, password } = payload;

  await http.post<SignupResponse>("/members/signup", {
    email,
    password,
    name: payload.nickname,
    // 화면이 필수 둘 다 체크해야 여기까지 온다. 서버도 둘 다 true 일 때만 기록한다
    agreedTerms: payload.agreements.includes("terms"),
    agreedPrivacy: payload.agreements.includes("privacy"),
  });

  const { role } = await http.post<LoginResponse>("/members/login", {
    email,
    password,
  });

  // 서버는 대표 포지션 하나를 position 으로 받는다. positions 배열은 지금 계약에 없다.
  // 보낸 항목만 바뀐다(ARC-120). 화면에 없는 webpage·profileImageUrl 은 싣지 않는다
  await http.patch<MemberProfileResponse>("/members/me", {
    nickname: payload.nickname,
    position: payload.position,
  });

  return loadAuthUser(role);
}

/** POST /api/v1/members/refresh — 쿠키의 refreshToken 으로 accessToken 재발급 */
export async function refreshAccessToken(refreshToken: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<LoginResponse>("/members/refresh", { refreshToken });
}

/** POST /api/v1/members/logout — 서버가 accessToken·refreshToken 쿠키를 지운다 */
export async function logout(): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<void>("/members/logout");
}

/**
 * 비밀번호 변경. 백엔드 미구현(기획서 11장 '회원관리 확장')이라 데모 동작만 한다.
 */
export async function changePassword(payload: {
  verificationCode: string;
  newPassword: string;
}): Promise<{ changed: boolean; message: string }> {
  const valid = payload.newPassword.length >= 8;
  return mockResponse({
    changed: valid,
    message: valid
      ? "비밀번호를 변경했어요. 다음 로그인부터 새 비밀번호를 사용해 주세요."
      : "비밀번호는 8자 이상이어야 해요.",
  });
}

/**
 * 이메일 인증번호 발송. 백엔드 미구현이라 데모에서는 언제나 성공하며 인증번호는 6자리면 통과한다.
 */
export async function requestEmailVerification(
  email: string,
): Promise<{ sent: boolean }> {
  return mockResponse({ sent: Boolean(email) });
}

/** 이메일 인증번호 확인. 백엔드 미구현. */
export async function confirmEmailVerification(
  email: string,
  code: string,
): Promise<{ verified: boolean; message: string }> {
  const verified = code.length === 6;
  return mockResponse({
    verified,
    message: verified
      ? "이메일 인증이 완료됐어요."
      : "인증번호 6자리를 입력해 주세요.",
  });
}
