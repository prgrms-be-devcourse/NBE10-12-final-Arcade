'use client';

import { usePathname } from 'next/navigation';
import { useReveal } from '@/lib/hooks/useReveal';
import { Header } from './Header';
import { Footer } from './Footer';
import { MobileNav } from './MobileNav';
import { AgreementGate } from '@/components/auth/AgreementGate';

/** 인증 화면은 콘텐츠만 보이는 공통 auth shell을 사용한다. */
const AUTH_ROUTES = ['/login', '/signup', '/forgot-password'];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  useReveal();

  const hideNav = AUTH_ROUTES.some((route) => pathname.startsWith(route));

  return (
    <div className="page app-shell">
      {hideNav ? null : <Header />}
      <div className="app-shell-content">{children}</div>
      {hideNav ? null : <Footer />}
      {hideNav ? null : <MobileNav />}
      {/* 약관 미동의 계정을 붙잡는다. 로그인·가입·약관 화면에서는 스스로 물러난다 */}
      <AgreementGate />
      {/*
        채팅 도크는 팀 논의 전까지 숨겨둔다.
        되살리려면 아래 두 줄의 주석을 풀면 된다 (컴포넌트는 components/team/ChatDock.tsx 에 그대로 있다).
        import { ChatDock } from '@/components/team/ChatDock';
        {hideNav ? null : <ChatDock />}
      */}
    </div>
  );
}
