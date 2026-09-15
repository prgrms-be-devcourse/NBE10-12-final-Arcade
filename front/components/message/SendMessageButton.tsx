'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { Modal } from '@/components/ui/Modal';
import { TextAreaField } from '@/components/ui/Field';
import { fetchMyProfileOrNull, sendDirectMessage } from '@/lib/api';
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
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [loginRequired, setLoginRequired] = useState(false);
  const [content, setContent] = useState('');
  const [result, setResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [sending, setSending] = useState(false);

  /**
   * 프로필·전시·파티 상세는 비로그인도 볼 수 있어서(SecurityConfig permitAll) 여기까지 들어온다.
   * 발송은 서버가 401 로 막지만, 다 써 놓고 거절당하지 않게 열 때 확인한다.
   *
   * 훅(useCurrentUser)을 쓰지 않는 이유는 이 버튼이 팀·지원자 목록에 행 수만큼 붙기 때문이다 -
   * 마운트마다 확인하면 목록을 여는 것만으로 그 수만큼 요청이 나간다. 누른 버튼만 한 번 묻는다.
   * 확인 자체가 실패(5xx)하면 비로그인으로 단정하지 않고 폼을 그대로 연다.
   */
  const openModal = async () => {
    const me = await fetchMyProfileOrNull().catch(() => undefined);
    setLoginRequired(me === null);
    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    setContent('');
    setResult(null);
    setLoginRequired(false);
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
          onClick={openModal}
        >
          <Icon name="i-mail" />
        </button>
      ) : (
        <button type="button" className="btn btn-ghost" onClick={openModal}>
          <Icon name="i-mail" />
          {label}
        </button>
      )}

      {open && loginRequired ? (
        <Modal
          open
          title="로그인이 필요해요"
          description="쪽지는 회원끼리 주고받아요. 로그인하면 바로 보낼 수 있어요."
          confirmLabel="로그인하러 가기"
          cancelLabel="닫기"
          onConfirm={() => router.push('/login')}
          onClose={close}
        />
      ) : null}

      {open && !loginRequired ? (
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
