'use client';

import { useState } from 'react';
import { FormGroup, TextField } from '@/components/ui/Field';
import { confirmEmailVerification, requestEmailVerification } from '@/lib/api';

interface EmailVerifyFieldProps {
  label: string;
  placeholder: string;
  idleMessage: string;
  value: string;
  onChange: (value: string) => void;
  onVerified?: (verified: boolean) => void;
}

/** 이메일 인증 요청 → 6자리 코드 확인 흐름 */
export function EmailVerifyField({
  label,
  placeholder,
  idleMessage,
  value,
  onChange,
  onVerified,
}: EmailVerifyFieldProps) {
  const [state, setState] = useState<'idle' | 'sent' | 'ok' | 'error'>('idle');
  const [message, setMessage] = useState(idleMessage);
  const [code, setCode] = useState('');

  const requestCode = async () => {
    if (!value) {
      setState('error');
      setMessage('이메일을 먼저 입력해 주세요.');
      return;
    }
    await requestEmailVerification(value);
    setState('sent');
    setMessage('인증 메일을 보냈어요. 메일함에서 6자리 인증번호를 확인해 주세요.');
  };

  const confirmCode = async () => {
    const result = await confirmEmailVerification(value, code);
    setState(result.verified ? 'ok' : 'error');
    setMessage(result.message);
    onVerified?.(result.verified);
  };

  return (
    <FormGroup label={label}>
      <div className="verify-row">
        <TextField
          type="email"
          placeholder={placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
        <button type="button" className="btn btn-ghost" onClick={requestCode}>
          인증 요청
        </button>
      </div>
      <p className="verify-status" data-state={state}>
        {message}
      </p>
      {state === 'sent' || state === 'error' ? (
        <div className="verify-row" style={{ marginTop: '0.5rem' }}>
          <TextField
            placeholder="메일로 받은 6자리 인증번호"
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
          <button type="button" className="btn btn-primary" onClick={confirmCode}>
            확인
          </button>
        </div>
      ) : null}
    </FormGroup>
  );
}
