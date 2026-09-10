'use client';

import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
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
 *  2. **403-3 수신** — 서버 필터가 막은 요청이 있으면 그때도 띄운다.
 *     1번을 통과했더라도(다른 탭에서 상태가 바뀌는 등) 서버가 최종 판단이다
 *
 * 서버도 같은 상태를 필터로 강제한다 - 화면만 막으면 API 를 직접 부르면 그만이다
 * (docs/약관동의-설계_개인정보보호법_ISMS.md 5장).
 */
export function AgreementGate() {
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [open, setOpen] = useState(false);

  // usePathname 이어야 한다. AppShell 은 상위 레이아웃이라 화면을 옮겨도 언마운트되지 않는데,
  // window.location.pathname 을 읽으면 마운트 시점의 경로에 그대로 묶인다 -
  // /login 에서 처음 뜬 뒤 로그인해서 / 로 이동해도 다시 확인하지 않는다.
  const pathname = usePathname();
  const skip = SKIP_ROUTES.some((route) => pathname.startsWith(route));

  useEffect(() => {
    if (skip) return;

    let cancelled = false;

    const apply = (me: UserProfile | null) => {
      // 비로그인(null)이면 띄울 이유가 없다. 조회 실패도 같이 걸러진다 -
      // 여기서 화면을 막으면 서버가 잠깐 흔들릴 때 전원이 갇힌다.
      if (cancelled || !me) return;

      setProfile(me);
      if (!me.privacyAgreedAt || !me.termsAgreedAt) setOpen(true);
    };

    const check = () => void fetchMyProfileOrNull().catch(() => null).then(apply);

    check();
    // 서버가 막았다는 신호를 받으면 다시 확인한다
    const unsubscribe = onAgreementRequired(check);

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [skip]);

  const submit = async (result: OnboardingResult) => {
    await completeOnboarding(result);
    setOpen(false);
    // 막혀서 비어 있던 화면을 다시 그린다
    router.refresh();
  };

  // skip 경로에서는 이미 열려 있어도 물러난다 - 약관 전문(/legal)을 보러 나간 동안
  // 모달이 그 위에 겹치면 읽을 수가 없다. 돌아오면 다시 뜬다.
  if (!open || skip) return null;

  return (
    <OnboardingModal
      open
      defaultNickname={profile?.nickname ?? profile?.name ?? ''}
      onSubmit={submit}
    />
  );
}
