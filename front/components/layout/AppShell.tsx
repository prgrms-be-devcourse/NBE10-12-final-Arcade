'use client';

import { usePathname } from 'next/navigation';
import { useReveal } from '@/lib/hooks/useReveal';
import { Header } from './Header';
import { Footer } from './Footer';
import { MobileNav } from './MobileNav';

/** 로그인 · 회원가입 화면에서는 네비게이션을 숨긴다 (목업의 AUTH_VIEWS) */
const AUTH_ROUTES = ['/login', '/signup'];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  useReveal();

  const hideNav = AUTH_ROUTES.some((route) => pathname.startsWith(route));

  return (
    <div className="page">
      {hideNav ? null : <Header />}
      {children}
      {hideNav ? null : <Footer />}
      {hideNav ? null : <MobileNav />}
      {/*
        채팅 도크는 팀 논의 전까지 숨겨둔다.
        되살리려면 아래 두 줄의 주석을 풀면 된다 (컴포넌트는 components/team/ChatDock.tsx 에 그대로 있다).
        import { ChatDock } from '@/components/team/ChatDock';
        {hideNav ? null : <ChatDock />}
      */}
    </div>
  );
}
