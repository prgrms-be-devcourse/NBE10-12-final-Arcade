'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { TextAreaField } from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { fetchMessages, sendDirectMessage, type MessageBoxName } from '@/lib/api';
import type { DirectMessage } from '@/lib/types';

const BOX_LABELS: Record<MessageBoxName, string> = {
  RECEIVED: '받은 쪽지',
  SENT: '보낸 쪽지',
};

const BOX_NAMES = Object.keys(BOX_LABELS) as MessageBoxName[];

/**
 * 마이페이지 쪽지함 — 받은함/보낸함 전환과 답장 폼.
 *
 * 서버에 답장 스레드가 없어서(쪽지는 한 방향 기록) 답장은 상대에게 보내는 새 쪽지로 처리한다.
 * 보낸 쪽지는 보낸함에서 확인할 수 있다.
 */
export function MessageBox({ messages: initial }: { messages: DirectMessage[] }) {
  // 서버 컴포넌트가 먼저 읽어 넘겨준 받은함으로 시작하고, 함을 바꿀 때만 다시 읽는다
  const [box, setBox] = useState<MessageBoxName>('RECEIVED');
  const [messages, setMessages] = useState(initial);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');

  const [replyingId, setReplyingId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [sendResult, setSendResult] = useState<{ ok: boolean; message: string } | null>(null);

  const changeBox = async (next: MessageBoxName) => {
    if (next === box) return;
    setBox(next);
    setReplyingId(null);
    setSendResult(null);
    setLoading(true);
    setLoadError('');
    try {
      setMessages(await fetchMessages({ box: next }));
    } catch {
      setMessages([]);
      setLoadError('쪽지함을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  const send = async (message: DirectMessage) => {
    const text = draft.trim();
    if (!text || sending) return;

    setSending(true);
    const result = await sendDirectMessage({
      recipientId: message.counterpartId ?? '',
      content: text,
    });
    setSending(false);
    setSendResult({ ok: result.sent, message: result.message });

    if (result.sent) {
      setReplyingId(null);
      setDraft('');
    }
  };

  return (
    <>
      <div className="block-head-row">
        <RadioChipGroup
          options={BOX_NAMES.map((name) => BOX_LABELS[name])}
          value={BOX_LABELS[box]}
          onChange={(label) =>
            changeBox(BOX_NAMES.find((name) => BOX_LABELS[name] === label) ?? box)
          }
        />
      </div>

      {loadError ? <p className="form-error">{loadError}</p> : null}

      <div className="msg-list">
        {messages.map((message) => (
          <div key={message.id} className={message.unread ? 'msg-item' : 'msg-item is-read'}>
            <span className="unread-dot" />
            <div className="msg-body">
              <div className="msg-top">
                <span className="msg-sender">
                  {box === 'SENT' ? `${message.from} 님에게` : message.from}
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
                    setSendResult(null);
                  }}
                >
                  <Icon name="i-mail" />
                  {box === 'SENT' ? '한 번 더 보내기' : '답장하기'}
                </button>
              </div>

              {replyingId === message.id ? (
                <div className="msg-reply-box">
                  <p className="msg-reply-to">
                    <b>{message.from}</b> 님에게 보내는 새 쪽지예요.
                  </p>
                  <TextAreaField
                    placeholder="보낼 내용을 입력하세요."
                    maxLength={1000}
                    value={draft}
                    onChange={(event) => {
                      setDraft(event.target.value);
                      setSendResult(null);
                    }}
                  />
                  {sendResult ? (
                    <p className="verify-status" data-state={sendResult.ok ? 'ok' : 'error'}>
                      {sendResult.message}
                    </p>
                  ) : null}
                  <div className="msg-reply-foot">
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => setReplyingId(null)}
                    >
                      취소
                    </button>
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={sending}
                      onClick={() => send(message)}
                    >
                      {sending ? '보내는 중…' : '보내기'}
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        ))}
      </div>

      {/* 답장 폼을 닫은 뒤에도 발송 결과는 한 번 보여준다 */}
      {sendResult?.ok && replyingId === null ? (
        <p className="verify-status" data-state="ok">
          {sendResult.message}
        </p>
      ) : null}

      {!loading && messages.length === 0 && !loadError ? (
        <p className="notif-empty">
          {box === 'RECEIVED' ? '도착한 쪽지가 없어요.' : '보낸 쪽지가 없어요.'}
        </p>
      ) : null}
    </>
  );
}
