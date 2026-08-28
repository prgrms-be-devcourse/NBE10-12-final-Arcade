import Link from 'next/link';

interface FooterProps {
  /** 마이페이지처럼 container 밖에서 쓰일 때 false */
  contained?: boolean;
}

export function Footer({ contained = true }: FooterProps) {
  return (
    <footer className={contained ? 'footer container' : 'footer'}>
      <span className="logo-text">
        CREW<span style={{ color: 'var(--accent)' }}>ON</span>
      </span>
      <p>© 2026 CrewOn. 실력으로 증명하는 팀 매칭 플랫폼.</p>
      <span style={{ display: 'flex', gap: '0.875rem', fontSize: '.82rem' }}>
        <Link href="/login" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
          로그인
        </Link>
        <Link href="/signup" className="auth-link-btn" style={{ color: 'var(--text-dim)' }}>
          회원가입
        </Link>
        {/*
          주최측 마이페이지(/host)·관리자 콘솔(/admin)은 기획서 11장의 '향후 구현' 항목이라
          이번 스코프에서는 링크를 노출하지 않는다. 라우트는 그대로 두어 직접 접근은 가능하다.
        */}
      </span>
    </footer>
  );
}
