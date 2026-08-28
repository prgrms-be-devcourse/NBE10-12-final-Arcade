'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
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

  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname.startsWith(href);

  return (
    <nav className={hidden ? 'nav is-hidden' : 'nav'}>
      <Link href="/" className="logo" aria-label="크루온 홈으로">
        <Icon name="i-joystick" className="logo-mark" />
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
            >
              {item.label}
            </Link>
          </li>
        ))}
      </ul>

      <div className="nav-right">
        <ThemeToggle />
        <MessagePanel />
        <NotificationPanel />
        <UserMenu />
      </div>
    </nav>
  );
}
