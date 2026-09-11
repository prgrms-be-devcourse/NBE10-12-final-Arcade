'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { logout } from '@/lib/api';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';

/**
 * 헤더 우측 프로필 메뉴 — 아바타 하나만 두고, 누르면 마이페이지·로그아웃이 펼쳐진다.
 * 관리자 계정은 마이페이지 자리에 관리자 페이지가 들어간다.
 *
 * 쪽지·알림 드롭다운과 같은 규칙을 따른다.
 * - 바깥을 누르거나 Esc 를 누르면 닫힌다
 * - 열려 있는 동안만 리스너를 걸어 둔다
 * - 메뉴에서 이동하면 먼저 닫는다 (돌아왔을 때 열린 채로 남지 않게)
 *
 * 로그아웃은 확인을 한 번 거치고, 요청이 실패해도 메인으로는 보낸다.
 * 로그인된 화면에 그대로 남아 있는 편이 더 위험하기 때문이다.
 *
 * 로그인하지 않았으면 로그인 버튼을, 로그인했으면 프로필 메뉴를 보여 준다.
 */
export function UserMenu() {
  const me = useCurrentUser();
  const router = useRouter();
  const wrapRef = useRef<HTMLDivElement>(null);
  const { confirm, dialog } = useConfirm();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (!wrapRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('click', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('click', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const go = (href: string) => {
    setOpen(false);
    router.push(href);
  };

  const runLogout = async () => {
    setOpen(false);
    const ok = await confirm({
      title: '로그아웃할까요?',
      description: '다시 이용하려면 로그인해야 해요.',
      confirmLabel: '로그아웃',
    });
    if (!ok) return;

    setPending(true);
    try {
      await logout();
    } finally {
      // 로그인 없이도 둘러볼 수 있는 서비스라 메인으로 보낸다.
      // router.push 로는 헤더(클라이언트 컴포넌트)가 다시 마운트되지 않아 로그인 상태가 그대로 남는다.
      window.location.href = '/';
    }
  };

  // 확인 중에는 아무것도 그리지 않는다 (로그인한 사람에게 로그인 버튼이 스치지 않게)
  if (me === undefined) return null;

  if (me === null) {
    return (
      <Link className="nav-login" href="/login">
        로그인
      </Link>
    );
  }

  const name = me.profile.name;
  const initial = me.profile.initial;

  // 관리자 계정은 운영용이라 메뉴에서 마이페이지 자리를 관리자 페이지가 대신한다.
  // 주소로는 여전히 /mypage 에 갈 수 있다 - 메뉴에서만 감춘다.
  const isAdmin = me.profile.memberRole === 'ADMIN';

  return (
    <div className="user-wrap" ref={wrapRef}>
        <button
          type="button"
          className={open ? 'user-chip is-open' : 'user-chip'}
          aria-label={`${name} 메뉴`}
          aria-haspopup="menu"
          aria-expanded={open}
          disabled={pending}
          onClick={(event) => {
            event.stopPropagation();
            setOpen((value) => !value);
          }}
        >
          <span className="mono">{initial}</span>
        </button>

        {open ? (
          <div className="user-menu" role="menu">
            <div className="user-menu-head">
              <span className="mono">{initial}</span>
              <span className="uname">{name}</span>
            </div>
            {isAdmin ? (
              <button
                type="button"
                className="user-menu-item"
                role="menuitem"
                onClick={() => go('/admin')}
              >
                <Icon name="i-crown" />
                관리자 페이지
              </button>
            ) : (
              <button
                type="button"
                className="user-menu-item"
                role="menuitem"
                onClick={() => go('/mypage')}
              >
                <Icon name="i-joystick" />
                마이페이지
              </button>
            )}
            <button type="button" className="user-menu-item is-danger" role="menuitem" onClick={runLogout} disabled={pending}>
              <Icon name="i-logout" />
              {pending ? '로그아웃 중…' : '로그아웃'}
            </button>
          </div>
        ) : null}

        {dialog}
    </div>
  );
}
