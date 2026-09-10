'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { OnboardingModal, type OnboardingResult } from './OnboardingModal';
import { completeOnboarding, fetchMyProfileOrNull } from '@/lib/api';
import { onAgreementRequired } from '@/lib/api/client';
import type { UserProfile } from '@/lib/types';

/** 온보딩을 띄우면 안 되는 화면. 로그인·가입은 아직 계정이 없거나 폼에서 동의를 받는다 */
const SKIP_ROUTES = ['/login', '/signup', '/legal'];

/**
 * 약관 미동의 계정을 붙잡아 온보딩으로 보내는 게이트 (기획서 2.1 · 3.4).
 *
 * GitHub 로그인은 가입 폼이 없어 동의를 받을 자리가 없다. 그래서 두 경로로 잡는다.
 *
 *  1. **진입 시 확인** — GET /members/me 의 privacyAgreedAt 이 null 이면 바로 띄운다
 *  2. **403-2 수신** — 서버 필터가 막은 요청이 있으면 그때도 띄운다.
 *     1번을 통과했더라도(다른 탭에서 상태가 바뀌는 등) 서버가 최종 판단이다
 *
 * 서버도 같은 상태를 필터로 강제한다 - 화면만 막으면 API 를 직접 부르면 그만이다
 * (docs/약관동의-설계_개인정보보호법_ISMS.md 5장).
 */
export function AgreementGate() {
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [open, setOpen] = useState(false);

  const check = useCallback(async () => {
    if (typeof window !== 'undefined' && SKIP_ROUTES.some((r) => window.location.pathname.startsWith(r))) {
      return;
    }

    // 비로그인(null)이면 띄울 이유가 없다. 조회 실패도 마찬가지로 조용히 넘긴다 -
    // 여기서 화면을 막으면 서버가 잠깐 흔들릴 때 전원이 갇힌다.
    const me = await fetchMyProfileOrNull().catch(() => null);
    if (!me) return;

    setProfile(me);
    if (!me.privacyAgreedAt || !me.termsAgreedAt) setOpen(true);
  }, []);

  useEffect(() => {
    void check();
    // 서버가 막았다는 신호를 받으면 다시 확인한다
    return onAgreementRequired(() => void check());
  }, [check]);

  const submit = async (result: OnboardingResult) => {
    await completeOnboarding(result);
    setOpen(false);
    // 막혀서 비어 있던 화면을 다시 그린다
    router.refresh();
  };

  if (!open) return null;

  return (
    <OnboardingModal
      open
      defaultNickname={profile?.nickname ?? profile?.name ?? ''}
      onSubmit={submit}
    />
  );
}
