'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon, type IconName } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import {
  deleteNotifications,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/lib/api';
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
  team: '/party/paybridge/team',
  detail: '/party/oakroom',
  mypageManage: '/mypage?tab=manage',
  mypageMessages: '/mypage?tab=messages',
  mypageIdentity: '/mypage?tab=identity',
  mypageBookmarks: '/mypage?tab=bookmarks',
  contests: '/contests',
};

/** 네비게이션 우측 알림 드롭다운 (선택 삭제 · 전체 읽음 포함) */
export function NotificationPanel() {
  const router = useRouter();
  const wrapRef = useRef<HTMLDivElement>(null);
  const { confirm, dialog } = useConfirm();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    fetchNotifications().then(setNotifications).catch(() => setNotifications([]));
  }, []);

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

  const markAll = async () => {
    setNotifications((prev) => prev.map((item) => ({ ...item, unread: false })));
    await markAllNotificationsRead();
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
    setNotifications((prev) => prev.filter((item) => !ids.includes(item.id)));
    setSelected([]);
    await deleteNotifications(ids);
  };

  const removeOne = async (id: string) => {
    const ok = await confirm({ title: '이 알림을 삭제할까요?' });
    if (!ok) return;

    setNotifications((prev) => prev.filter((item) => item.id !== id));
    setSelected((prev) => prev.filter((value) => value !== id));
    await deleteNotifications([id]);
  };

  const openNotification = async (notification: AppNotification) => {
    setNotifications((prev) =>
      prev.map((item) => (item.id === notification.id ? { ...item, unread: false } : item)),
    );
    await markNotificationRead(notification.id);
    setOpen(false);
    router.push(NOTIF_ROUTES[notification.target]);
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
        </div>
      ) : null}
    </div>
  );
}
