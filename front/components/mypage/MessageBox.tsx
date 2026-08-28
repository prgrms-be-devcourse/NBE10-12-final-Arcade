'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { TextAreaField } from '@/components/ui/Field';
import { replyToMessage } from '@/lib/api';
import type { DirectMessage } from '@/lib/types';

/** 마이페이지 쪽지함 — 답장 인라인 폼 포함 */
export function MessageBox({ messages: initial }: { messages: DirectMessage[] }) {
  const [messages, setMessages] = useState(initial);
  const [replyingId, setReplyingId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');

  const send = async (id: string) => {
    const text = draft.trim();
    if (!text) return;
    const reply = await replyToMessage(id, text);
    setMessages((prev) =>
      prev.map((message) =>
        message.id === id ? { ...message, replies: [...message.replies, reply] } : message,
      ),
    );
    setReplyingId(null);
    setDraft('');
  };

  return (
    <div className="msg-list">
      {messages.map((message) => (
        <div key={message.id} className={message.unread ? 'msg-item' : 'msg-item is-read'}>
          <span className="unread-dot" />
          <div className="msg-body">
            <div className="msg-top">
              <span className="msg-sender">
                {message.from}
                {message.role ? ` · ${message.role}` : ''}
              </span>
              <span className="msg-time">{message.time}</span>
            </div>
            <p className="msg-preview">{message.text}</p>

            {message.replies.map((reply) => (
              <p key={reply.id} className="msg-preview" style={{ color: 'var(--text-dim)' }}>
                ↳ {reply.text} ({reply.time})
              </p>
            ))}

            <div className="msg-actions">
              <button
                type="button"
                className="msg-reply-btn"
                onClick={() => {
                  setReplyingId(replyingId === message.id ? null : message.id);
                  setDraft('');
                }}
              >
                <Icon name="i-mail" />
                답장하기
              </button>
            </div>

            {replyingId === message.id ? (
              <div className="msg-reply-box">
                <p className="msg-reply-to">
                  <b>{message.from}</b> 님에게 답장
                </p>
                <TextAreaField
                  placeholder="답장 내용을 입력하세요."
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                />
                <div className="msg-reply-foot">
                  <button type="button" className="btn btn-ghost" onClick={() => setReplyingId(null)}>
                    취소
                  </button>
                  <button type="button" className="btn btn-primary" onClick={() => send(message.id)}>
                    보내기
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  );
}
