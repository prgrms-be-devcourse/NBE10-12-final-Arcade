'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { fetchMessages, markAllMessagesRead, markMessageRead } from '@/lib/api';
import type { DirectMessage } from '@/lib/types';

/** 네비게이션 우측 쪽지 드롭다운 */
export function MessagePanel() {
  const router = useRouter();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<DirectMessage[]>([]);

  useEffect(() => {
    fetchMessages().then(setMessages).catch(() => setMessages([]));
  }, []);

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (!wrapRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('click', onClick);
    return () => document.removeEventListener('click', onClick);
  }, [open]);

  const unread = messages.filter((message) => message.unread).length;

  const markAll = async () => {
    setMessages((prev) => prev.map((message) => ({ ...message, unread: false })));
    await markAllMessagesRead();
  };

  const openMessage = async (id: string) => {
    setMessages((prev) =>
      prev.map((message) => (message.id === id ? { ...message, unread: false } : message)),
    );
    await markMessageRead(id);
    setOpen(false);
    router.push('/mypage?tab=messages');
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
