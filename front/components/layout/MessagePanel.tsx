'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { fetchMessages, markMessagesRead } from '@/lib/api';
import { useRefreshOnVisible } from '@/lib/hooks/useRefreshOnVisible';
import type { DirectMessage } from '@/lib/types';

/** 네비게이션 우측 쪽지 드롭다운 */
export function MessagePanel() {
  const router = useRouter();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<DirectMessage[]>([]);

  /** 받은 쪽지 다시 읽기. 배경 갱신이 실패하면 화면에 있던 것을 그대로 둔다 */
  const load = useCallback(() => {
    fetchMessages()
      .then(setMessages)
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // 알림 패널과 같은 규칙 — 창을 다시 볼 때 읽고, 열어둔 동안은 건너뛴다
  useRefreshOnVisible(
    useCallback(() => {
      if (!open) load();
    }, [open, load]),
  );

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (!wrapRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('click', onClick);
    return () => document.removeEventListener('click', onClick);
  }, [open]);

  const unread = messages.filter((message) => message.unread).length;

  /**
   * PATCH /members/me/messages — 서버에 '전체 읽음' 이 따로 없어서 안 읽은 것들의 id 를 모아 보낸다.
   * 먼저 화면을 바꾸고, 실패하면 되돌린다.
   */
  const markAll = async () => {
    const unreadIds = messages.filter((message) => message.unread).map((message) => message.id);
    if (unreadIds.length === 0) return;

    const previous = messages;
    setMessages((prev) => prev.map((message) => ({ ...message, unread: false })));
    try {
      await markMessagesRead(unreadIds);
    } catch {
      setMessages(previous);
    }
  };

  const openMessage = async (id: string) => {
    setOpen(false);
    router.push('/mypage?tab=messages');

    // 이미 읽은 쪽지는 다시 보내지 않는다. 읽음 처리가 실패해도 이동은 그대로 둔다
    if (!messages.find((message) => message.id === id)?.unread) return;

    const previous = messages;
    setMessages((prev) =>
      prev.map((message) => (message.id === id ? { ...message, unread: false } : message)),
    );
    try {
      await markMessagesRead([id]);
    } catch {
      setMessages(previous);
    }
  };

  return (
    <div className="msg-wrap" ref={wrapRef}>
      <button
        type="button"
        className="icon-btn"
        aria-label="쪽지함"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={(event) => {
          event.stopPropagation();
          // 열 때 한 번 읽어둔다
          if (!open) load();
          setOpen((value) => !value);
        }}
      >
        <Icon name="i-mail" />
        {unread > 0 ? <span className="notif-badge">{unread > 9 ? '9+' : unread}</span> : null}
      </button>

      {open ? (
        <div className="notif-panel msg-panel">
          <div className="notif-panel-head">
            <h4>도착한 쪽지</h4>
            <div className="notif-panel-actions">
              <button type="button" className="notif-action-btn" onClick={markAll}>
                전체 읽음
              </button>
            </div>
          </div>

          <div className="notif-list">
            {messages.map((message) => (
              <div
                key={message.id}
                className="msg-drop-item"
                data-unread={message.unread ? 'true' : 'false'}
                role="button"
                tabIndex={0}
                onClick={() => openMessage(message.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') openMessage(message.id);
                }}
              >
                <span className="msg-drop-avatar">{message.initial}</span>
                <div className="msg-drop-body">
                  <div className="msg-drop-top">
                    <span className="msg-drop-name">{message.from}</span>
                    <span className="msg-drop-time">{message.time}</span>
                  </div>
                  <p className="msg-drop-text">{message.text}</p>
                </div>
              </div>
            ))}
          </div>
          {messages.length === 0 ? <p className="notif-empty">새 쪽지가 없어요.</p> : null}

          <div className="msg-panel-foot">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setOpen(false);
                router.push('/mypage?tab=messages');
              }}
            >
              쪽지함 전체보기
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
