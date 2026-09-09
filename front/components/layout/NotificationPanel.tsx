'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon, type IconName } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { deleteNotifications, fetchNotifications, markNotificationsRead } from '@/lib/api';
import { useServerEvents } from '@/lib/hooks/useServerEvents';
import type { AppNotification, NotificationTarget, NotificationType } from '@/lib/types';

const NOTIF_ICONS: Record<NotificationType, IconName> = {
  approval: 'i-check',
  applicant: 'i-users',
  deadline: 'i-clock',
  message: 'i-mail',
  achievement: 'i-trophy',
  contest: 'i-crown',
  comment: 'i-mail',
};

const NOTIF_ROUTES: Record<NotificationTarget, string> = {
  mypageManage: '/mypage?tab=manage',
  mypageMessages: '/mypage?tab=messages',
  mypageIdentity: '/mypage?tab=identity',
  mypageBookmarks: '/mypage?tab=bookmarks',
  contests: '/contests',
  exhibition: '/exhibition',
};

/** 한 번에 읽어오는 건수. 서버 기본값과 같지만 화면이 정한 값임을 드러낸다 */
const PAGE_SIZE = 20;

/** 네비게이션 우측 알림 드롭다운 (선택 삭제 · 전체 읽음 포함) */
export function NotificationPanel() {
  const router = useRouter();
  const wrapRef = useRef<HTMLDivElement>(null);
  const { confirm, dialog } = useConfirm();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  /** 마지막으로 읽어온 쪽(0부터) */
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  /** 첫 쪽부터 다시 읽기. 배경 갱신이 실패하면 화면에 있던 것을 그대로 둔다 */
  const load = useCallback(() => {
    fetchNotifications({ size: PAGE_SIZE })
      .then((result) => {
        setNotifications(result.items);
        setPage(0);
        setHasMore(result.totalPages > 1);
      })
      .catch(() => undefined);
  }, []);

  /**
   * 다음 쪽을 뒤에 이어 붙인다.
   *
   * 알림은 이 드롭다운이 전부라 따로 목록 화면이 없다 — 쪽을 넘기는 대신 쌓아 보여준다.
   * 예전에는 서버 기본값 한 쪽(20건)만 읽어 21번째부터는 영영 볼 수 없었다.
   */
  const loadMore = () => {
    if (loadingMore) return;
    const next = page + 1;
    setLoadingMore(true);
    fetchNotifications({ page: next, size: PAGE_SIZE })
      .then((result) => {
        setNotifications((prev) => [...prev, ...result.items]);
        setPage(next);
        setHasMore(next + 1 < result.totalPages);
      })
      .catch(() => undefined)
      .finally(() => setLoadingMore(false));
  };

  useEffect(() => {
    load();
  }, [load]);

  /*
   * 새 알림은 서버가 SSE 로 밀어준다 (GET /notifications/subscribe).
   *
   * 패널을 열어둔 동안은 다시 읽지 않는다 — 읽는 중에 목록이 바뀌면 선택해 둔 항목이 어긋난다.
   * 끊겼다 다시 붙으면 그동안의 이벤트는 사라지므로, connect 때 목록을 통째로 다시 읽어 메꾼다.
   */
  useServerEvents('/notifications/subscribe', {
    connect: () => {
      if (!open) load();
    },
    notification: () => {
      if (!open) load();
    },
  });

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (!wrapRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('click', onClick);
    return () => document.removeEventListener('click', onClick);
  }, [open]);

  const unread = notifications.filter((item) => item.unread).length;
  const allSelected = notifications.length > 0 && selected.length === notifications.length;

  const toggleSelectAll = () =>
    setSelected(allSelected ? [] : notifications.map((item) => item.id));

  const toggleOne = (id: string) =>
    setSelected((prev) => (prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]));

  /**
   * PATCH /notifications/read — 서버에 '전체 읽음' 이 따로 없어서 안 읽은 것들의 id 를 모아 보낸다.
   * 먼저 화면을 바꾸고, 실패하면 되돌린다.
   */
  const markAll = async () => {
    const unreadIds = notifications.filter((item) => item.unread).map((item) => item.id);
    if (unreadIds.length === 0) return;

    const previous = notifications;
    setNotifications((prev) => prev.map((item) => ({ ...item, unread: false })));
    try {
      await markNotificationsRead(unreadIds);
    } catch {
      setNotifications(previous);
    }
  };

  /** DELETE /notifications { ids: [...] } */
  const removeSelected = async () => {
    if (selected.length === 0) return;
    const ok = await confirm({
      title: `알림 ${selected.length}건을 삭제할까요?`,
      description: '삭제한 알림은 되돌릴 수 없어요.',
    });
    if (!ok) return;

    const ids = [...selected];
    const previous = notifications;
    setNotifications((prev) => prev.filter((item) => !ids.includes(item.id)));
    setSelected([]);
    try {
      await deleteNotifications(ids);
    } catch {
      setNotifications(previous);
      setSelected(ids);
    }
  };

  const removeOne = async (id: string) => {
    const ok = await confirm({ title: '이 알림을 삭제할까요?' });
    if (!ok) return;

    const previous = notifications;
    setNotifications((prev) => prev.filter((item) => item.id !== id));
    setSelected((prev) => prev.filter((value) => value !== id));
    try {
      await deleteNotifications([id]);
    } catch {
      setNotifications(previous);
    }
  };

  const openNotification = async (notification: AppNotification) => {
    setOpen(false);
    router.push(NOTIF_ROUTES[notification.target]);

    // 이미 읽은 알림은 다시 보내지 않는다. 읽음 처리가 실패해도 이동은 그대로 둔다
    if (!notification.unread) return;

    const previous = notifications;
    setNotifications((prev) =>
      prev.map((item) => (item.id === notification.id ? { ...item, unread: false } : item)),
    );
    try {
      await markNotificationsRead([notification.id]);
    } catch {
      setNotifications(previous);
    }
  };

  return (
    <div className="notif-wrap" ref={wrapRef}>
      {dialog}
      <button
        type="button"
        className="icon-btn"
        aria-label="알림"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={(event) => {
          event.stopPropagation();
          // 열 때 한 번 읽어둔다 — 열어보는 순간이 가장 최신을 원하는 시점이다
          if (!open) load();
          setOpen((value) => !value);
        }}
      >
        <Icon name="i-bell" />
        {unread > 0 ? <span className="notif-badge">{unread > 9 ? '9+' : unread}</span> : null}
      </button>

      {open ? (
        <div className="notif-panel">
          <div className="notif-panel-head">
            <h4>알림</h4>
            <div className="notif-panel-actions">
              <button type="button" className="notif-action-btn" onClick={markAll}>
                전체 읽음
              </button>
            </div>
          </div>

          <div className="notif-select-bar">
            <span
              className={allSelected ? 'notif-check is-checked' : 'notif-check'}
              role="checkbox"
              aria-checked={allSelected}
              tabIndex={0}
              onClick={toggleSelectAll}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') toggleSelectAll();
              }}
            >
              <Icon name="i-check" />
            </span>
            <span className="notif-select-label" onClick={toggleSelectAll}>
              전체 선택
            </span>
            <span className="notif-select-count">{selected.length}개 선택됨</span>
            <button
              type="button"
              className="notif-group-del"
              disabled={selected.length === 0}
              onClick={removeSelected}
            >
              {selected.length ? `선택 삭제 (${selected.length})` : '선택 삭제'}
            </button>
          </div>

          <div className="notif-list">
            {notifications.map((notification) => {
              const checked = selected.includes(notification.id);
              return (
                <div
                  key={notification.id}
                  className="notif-item"
                  data-unread={notification.unread ? 'true' : 'false'}
                  role="button"
                  tabIndex={0}
                  onClick={() => openNotification(notification)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') openNotification(notification);
                  }}
                >
                  <span
                    className={checked ? 'notif-check is-checked' : 'notif-check'}
                    role="checkbox"
                    aria-checked={checked}
                    onClick={(event) => {
                      event.stopPropagation();
                      toggleOne(notification.id);
                    }}
                  >
                    <Icon name="i-check" />
                  </span>
                  <span className="notif-icon">
                    <Icon name={NOTIF_ICONS[notification.type]} />
                  </span>
                  <div className="notif-body">
                    <p className="notif-text">{notification.text}</p>
                    <p className="notif-time">{notification.time}</p>
                  </div>
                  <button
                    type="button"
                    className="notif-close"
                    aria-label="알림 닫기"
                    onClick={(event) => {
                      event.stopPropagation();
                      removeOne(notification.id);
                    }}
                  >
                    <Icon name="i-x" />
                  </button>
                </div>
              );
            })}
          </div>
          {notifications.length === 0 ? <p className="notif-empty">새 알림이 없어요.</p> : null}
          {hasMore ? (
            <button type="button" className="notif-more" disabled={loadingMore} onClick={loadMore}>
              {loadingMore ? '불러오는 중…' : '이전 알림 더 보기'}
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
