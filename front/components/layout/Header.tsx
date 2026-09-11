'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState, type CSSProperties } from 'react';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { useHideOnScroll } from '@/lib/hooks/useHideOnScroll';
import { MessagePanel } from './MessagePanel';
import { NotificationPanel } from './NotificationPanel';
import { ThemeToggle } from './ThemeToggle';
import { UserMenu } from './UserMenu';

const NAV_ITEMS = [
  { href: '/', label: '홈' },
  { href: '/party', label: '파티' },
  { href: '/contests', label: '공모전/대회' },
  { href: '/exhibition', label: '전시관' },
];

export function Header() {
  const pathname = usePathname();
  const hidden = useHideOnScroll();
  const me = useCurrentUser();
  const [logoMotion, setLogoMotion] = useState<'idle' | 'enter' | 'hover' | 'release'>('idle');
  const [logoRelease, setLogoRelease] = useState({ from: '12deg', coast: '16deg', rebound: '-8deg' });

  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname.startsWith(href);

  /**
   * 반복 애니메이션의 경과 시간으로 현재 각도와 진행 방향을 계산한다.
   * CSS transform 행렬을 읽으면 SVG <g>에서 브라우저별로 none을 돌려주는 경우가 있어,
   * 같은 easing 곡선을 수식으로 재현해 복귀 시작점과 관성을 안정적으로 맞춘다.
   */
  const releaseLogo = (element: HTMLElement) => {
    const stick = element.querySelector<SVGGElement>('.logo-joystick-stick');
    const elapsed = Number(stick?.getAnimations()[0]?.currentTime ?? 0);
    let angle = -13;
    let direction = -1;
    let speed = 0;

    if (logoMotion === 'hover') {
      const duration = 360;
      const iteration = Math.floor(elapsed / duration);
      const progress = (elapsed % duration) / duration;
      // ease-in-out과 같은 형태: 끝에서는 감속하고 가운데에서 가장 빠르다.
      const eased = (1 - Math.cos(Math.PI * progress)) / 2;
      const forward = iteration % 2 === 0;
      const position = forward ? eased : 1 - eased;
      angle = -13 + position * 26;
      direction = forward ? 1 : -1;
      speed = Math.sin(Math.PI * progress);
    } else if (logoMotion === 'enter') {
      const progress = Math.min(elapsed / 180, 1);
      angle = -13 * progress;
      direction = -1;
      speed = 1 - progress;
    }

    const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));
    // 속도가 빠를수록 현재 진행 방향으로 조금 더 간 뒤 반대편으로 반동한다.
    const coast = clamp(angle + direction * (2 + speed * 4), -16, 16);
    const rebound = clamp(-coast * 0.55, -9, 9);
    setLogoRelease({ from: `${angle}deg`, coast: `${coast}deg`, rebound: `${rebound}deg` });
    setLogoMotion('release');
  };

  return (
    <nav className={hidden ? 'nav is-hidden' : 'nav'}>
      <Link
        href="/"
        className={`logo logo--${logoMotion}`}
        style={{ '--logo-release-from': logoRelease.from, '--logo-release-coast': logoRelease.coast, '--logo-release-rebound': logoRelease.rebound } as CSSProperties}
        aria-label="크루온 홈으로"
        onMouseEnter={() => setLogoMotion('enter')}
        onMouseLeave={(event) => releaseLogo(event.currentTarget)}
        onFocus={() => setLogoMotion('enter')}
        onBlur={(event) => releaseLogo(event.currentTarget)}
        onAnimationEnd={(event) => {
          if (event.animationName === 'logo-joystick-enter') setLogoMotion('hover');
          if (event.animationName === 'logo-joystick-release') setLogoMotion('idle');
        }}
      >
        <svg className="logo-mark" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="square" strokeLinejoin="miter" aria-hidden="true">
          <g className="logo-joystick-stick">
            <circle cx="12" cy="6" r="3" />
            <path d="M12 9v7" />
          </g>
          <path d="M7 20h10l-1.5-4h-7z" />
        </svg>
        <span className="logo-text">
          CREW<span>ON</span>
        </span>
      </Link>

      <ul className="nav-menu">
        {NAV_ITEMS.map((item) => (
          <li key={item.href}>
            <Link
              href={item.href}
              className={isActive(item.href) ? 'nav-link is-active' : 'nav-link'}
              aria-current={isActive(item.href) ? 'page' : undefined}
            >
              {item.label}
            </Link>
          </li>
        ))}
      </ul>

      <div className="nav-right">
        <ThemeToggle />
        {/* 쪽지·알림은 개인 기능이라 로그인한 경우에만 보여준다 */}
        {me ? <MessagePanel /> : null}
        {me ? <NotificationPanel /> : null}
        <UserMenu />
      </div>
    </nav>
  );
}
