'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, TextField } from '@/components/ui/Field';
import {
  changePassword,
  confirmEmailVerification,
  requestEmailVerification,
} from '@/lib/api';

type Step = 'request' | 'verify' | 'reset' | 'done';

const STEP_CONFIRM: Record<Step, string | undefined> = {
  request: '인증번호 받기',
  verify: '인증 확인',
  reset: '비밀번호 변경',
  done: undefined,
};

/**
 * 비밀번호 변경 다이얼로그.
 *
 * 프로필 수정 폼과 분리해 별도 창으로 띄운다 — 비밀번호 변경은 누르는 즉시 서버에 반영되는데,
 * 취소·저장 버튼이 있는 폼 안에 있으면 저장을 눌러야 반영되는 것처럼 보이기 때문이다.
 *
 * 열려 있을 때만 렌더링되므로, 다시 열면 자연스럽게 첫 단계부터 시작한다.
 */
export function PasswordChangeModal({ email, onClose }: { email: string; onClose: () => void }) {
  const [step, setStep] = useState<Step>('request');
  const [message, setMessage] = useState('');
  const [messageState, setMessageState] = useState<'idle' | 'ok' | 'error'>('idle');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const fail = (text: string) => {
    setMessageState('error');
    setMessage(text);
  };

  const next = async () => {
    if (step === 'request') {
      await requestEmailVerification(email);
      setStep('verify');
      setMessageState('idle');
      setMessage(`${email} 로 인증번호를 보냈어요. 메일함을 확인해 주세요.`);
      return;
    }

    if (step === 'verify') {
      const result = await confirmEmailVerification(email, code);
      if (!result.verified) return fail(result.message);
      setStep('reset');
      setMessageState('ok');
      setMessage(result.message);
      return;
    }

    if (step === 'reset') {
      if (password !== passwordConfirm) return fail('비밀번호가 일치하지 않아요.');
      const result = await changePassword({ verificationCode: code, newPassword: password });
      if (!result.changed) return fail(result.message);
      setStep('done');
      setMessageState('ok');
      setMessage(result.message);
    }
  };

  return (
    <Modal
      open
      title="비밀번호 변경"
      description="본인 확인을 위해 이메일 인증을 거친 뒤 새 비밀번호를 설정해요."
      confirmLabel={STEP_CONFIRM[step]}
      cancelLabel={step === 'done' ? '닫기' : '취소'}
      onConfirm={next}
      onClose={onClose}
    >
      <FormGroup label="가입 이메일">
        <TextField value={email} readOnly />
      </FormGroup>

      {step === 'verify' ? (
        <FormGroup label="인증번호">
          <TextField
            placeholder="메일로 받은 6자리 인증번호"
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value)}
            autoFocus
          />
        </FormGroup>
      ) : null}

      {step === 'reset' ? (
        <>
          <FormGroup label="새 비밀번호">
            <TextField
              type="password"
              placeholder="8자 이상"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoFocus
            />
          </FormGroup>
          <FormGroup label="새 비밀번호 확인">
            <TextField
              type="password"
              placeholder="비밀번호 재입력"
              value={passwordConfirm}
              onChange={(event) => setPasswordConfirm(event.target.value)}
            />
          </FormGroup>
        </>
      ) : null}

      {message ? (
        <p className="verify-status" data-state={messageState}>
          {message}
        </p>
      ) : null}
    </Modal>
  );
}
