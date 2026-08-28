import Link from 'next/link';
import type { UserSummary } from '@/lib/types';

interface AvatarProps {
  initial: string;
  /** 있으면 사진으로, 없으면 이니셜로 그린다 */
  avatarUrl?: string;
  size?: 'default' | 'small';
}

export function Avatar({ initial, avatarUrl, size = 'default' }: AvatarProps) {
  return (
    <div className={size === 'small' ? 'avatar-frame small' : 'avatar-frame'}>
      <div
        className={avatarUrl ? 'inner has-photo' : 'inner'}
        style={avatarUrl ? { backgroundImage: `url(${avatarUrl})` } : undefined}
      >
        {avatarUrl ? null : initial}
      </div>
    </div>
  );
}

export function MiniAvatar({ initial, avatarUrl }: { initial: string | null; avatarUrl?: string }) {
  const className = [
    'mini-avatar',
    initial ? null : 'is-unassigned',
    avatarUrl ? 'has-photo' : null,
  ]
    .filter(Boolean)
    .join(' ');
  return (
    <span className={className} style={avatarUrl ? { backgroundImage: `url(${avatarUrl})` } : undefined}>
      {avatarUrl ? null : (initial ?? '?')}
    </span>
  );
}

interface LeaderRowProps {
  user: UserSummary;
  /** 지정하면 프로필로 이동하는 링크가 된다 */
  href?: string;
  role?: string;
  card?: boolean;
}

/** 아바타(사진 없으면 이니셜) + 이름/역할 — 카드 하단, 팀원 목록 등에서 재사용 */
export function LeaderRow({ user, href, role, card }: LeaderRowProps) {
  const className = card ? 'leader-row leader-card-row' : 'leader-row';
  const inner = (
    <>
      <span
        className={user.avatarUrl ? 'leader-avatar has-photo' : 'leader-avatar'}
        style={user.avatarUrl ? { backgroundImage: `url(${user.avatarUrl})` } : undefined}
      >
        {user.avatarUrl ? null : user.initial}
      </span>
      <span className="leader-info">
        <span className="name">{user.name}</span>
        <span className="role">{role ?? user.role}</span>
      </span>
    </>
  );

  if (href) {
    return (
      <Link href={href} className={className}>
        {inner}
      </Link>
    );
  }
  return <span className={className}>{inner}</span>;
}
