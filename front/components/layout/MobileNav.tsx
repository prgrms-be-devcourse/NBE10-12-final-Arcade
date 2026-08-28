'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Icon, type IconName } from '@/components/icons/Icon';

const ITEMS: { href: string; label: string; icon: IconName }[] = [
  { href: '/', label: '홈', icon: 'i-joystick' },
  { href: '/party', label: '파티', icon: 'i-users' },
  { href: '/contests', label: '공모전', icon: 'i-trophy' },
  { href: '/exhibition', label: '전시관', icon: 'i-eye' },
];

/**
 * 모바일 전용 하단 메뉴바.
 *
 * 좁은 화면에서는 헤더의 가로 메뉴가 들어가지 않아 숨겨진다(.nav-menu).
 * 그 자리를 이 떠 있는 바가 대신한다 — 헤더가 스크롤로 숨어도 이동은 항상 가능하다.
 * 데스크톱에서는 CSS 로 감춘다(화면 폭에 따라 렌더링을 바꾸면 하이드레이션이 어긋난다).
 */
export function MobileNav() {
  const pathname = usePathname();
  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname.startsWith(href);

  return (
    <nav className="mobile-nav" aria-label="주요 메뉴">
      {ITEMS.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className={isActive(item.href) ? 'mobile-nav-item is-active' : 'mobile-nav-item'}
          aria-current={isActive(item.href) ? 'page' : undefined}
        >
          <Icon name={item.icon} />
          <span>{item.label}</span>
        </Link>
      ))}
    </nav>
  );
}
