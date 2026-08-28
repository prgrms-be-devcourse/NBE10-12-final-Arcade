'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { Modal } from '@/components/ui/Modal';
import { TextAreaField } from '@/components/ui/Field';
import { sendDirectMessage } from '@/lib/api';
import type { UserSummary } from '@/lib/types';

const MAX_LENGTH = 500;

interface SendMessageButtonProps {
  recipient: UserSummary;
  /** button = 본문용 큰 버튼, icon = 목록 행에 붙는 작은 버튼 */
  variant?: 'button' | 'icon';
  /** 버튼 문구 — 매칭 전 문의처럼 맥락이 다를 때 바꾼다 */
  label?: string;
}

/**
 * 쪽지 보내기 (기획서 2.8).
 * 전시·프로필을 보고 바로 연락할 수 있게, 상대를 보고 있는 화면에서 곧장 열린다.
 */
export function SendMessageButton({
  recipient,
  variant = 'button',
  label = '쪽지 보내기',
}: SendMessageButtonProps) {
  const [open, setOpen] = useState(false);
  const [content, setContent] = useState('');
  const [result, setResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [sending, setSending] = useState(false);

  const close = () => {
    setOpen(false);
    setContent('');
    setResult(null);
  };

  const submit = async () => {
    if (!content.trim()) {
      setResult({ ok: false, message: '내용을 입력해 주세요.' });
      return;
    }
    setSending(true);
    try {
      const response = await sendDirectMessage({ recipientId: recipient.id, content });
      setResult({ ok: response.sent, message: response.message });
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      {variant === 'icon' ? (
        <button
          type="button"
          className="msg-send-icon"
          aria-label={`${recipient.name}님에게 쪽지 보내기`}
          title={`${recipient.name}님에게 쪽지 보내기`}
          onClick={() => setOpen(true)}
        >
          <Icon name="i-mail" />
        </button>
      ) : (
        <button type="button" className="btn btn-ghost" onClick={() => setOpen(true)}>
          <Icon name="i-mail" />
          {label}
        </button>
      )}

      {open ? (
        <Modal
          open
          title={`${recipient.name}님에게 쪽지`}
          description="실시간 채팅이 아니라 비동기 메시지예요. 상대가 확인하면 답장이 쪽지함으로 옵니다."
          confirmLabel={result?.ok ? undefined : sending ? '보내는 중…' : '보내기'}
          cancelLabel={result?.ok ? '닫기' : '취소'}
          onConfirm={submit}
          onClose={close}
        >
          <div className="msg-send-to">
            <span className="mini-avatar">{recipient.initial}</span>
            <span>
              <b>{recipient.name}</b>
              <span className="msg-send-role">{recipient.role}</span>
            </span>
          </div>

          {result?.ok ? null : (
            <>
              <TextAreaField
                placeholder="어떤 점이 인상 깊었는지, 무엇을 제안하고 싶은지 적어주세요."
                maxLength={MAX_LENGTH}
                value={content}
                onChange={(event) => {
                  setContent(event.target.value);
                  setResult(null);
                }}
                autoFocus
              />
              <p className="char-count">
                {content.length} / {MAX_LENGTH}
              </p>
            </>
          )}

          {result ? (
            <p className="verify-status" data-state={result.ok ? 'ok' : 'error'}>
              {result.message}
            </p>
          ) : null}
        </Modal>
      ) : null}
    </>
  );
}
