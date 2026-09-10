'use client';

import Link from 'next/link';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';

interface FooterProps {
  /** 마이페이지처럼 container 밖에서 쓰일 때 false */
  contained?: boolean;
}

export function Footer({ contained = true }: FooterProps) {
  const me = useCurrentUser();

  return (
    <footer className={contained ? 'footer container' : 'footer'}>
      <span className="logo-text">
        CREW<span style={{ color: 'var(--accent)' }}>ON</span>
      </span>
      <p>© 2026 CrewOn. 실력으로 증명하는 팀 매칭 플랫폼.</p>
      <span style={{ display: 'flex', gap: '0.875rem', fontSize: '.82rem' }}>
        {/* 로그인한 사람에게는 헤더에 이미 이동 수단이 있어 푸터에서는 반복하지 않는다 */}
        {me ? null : (
          <>
            <Link href="/login" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
              로그인
            </Link>
            <Link href="/signup" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
              회원가입
            </Link>
          </>
        )}
        {/* 약관·개인정보 처리방침은 가입 화면 밖에서도 언제든 볼 수 있어야 한다 */}
        <Link href="/legal/terms" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
          이용약관
        </Link>
        <Link href="/legal/privacy" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
          개인정보처리방침
        </Link>

        {/*
          주최측 마이페이지(/host)·관리자 콘솔(/admin)은 기획서 11장의 '향후 구현' 항목이라
          이번 스코프에서는 링크를 노출하지 않는다. 라우트는 그대로 두어 직접 접근은 가능하다.
        */}
      </span>
    </footer>
  );
}
