'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { AuthLogo } from './AuthLogo';
import { FormGroup, TextField } from '@/components/ui/Field';
import { ApiError, requestPasswordReset } from '@/lib/api';

export function ForgotPasswordForm() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    setError('');
    setSubmitting(true);
    try {
      await requestPasswordReset(email.trim());
      setSent(true);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : '메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <AuthLogo />
        <h1 className="auth-title">비밀번호 찾기</h1>
        <p className="auth-subtitle">가입한 이메일로 30분 동안 유효한 비밀번호 재설정 링크를 보내드려요.</p>

        {sent ? (
          <div className="password-reset-done" role="status">
            <span aria-hidden="true">✓</span>
            <h2>메일을 확인해 주세요</h2>
            <p>가입된 계정이라면 <b>{email}</b>로 재설정 링크를 보냈어요.<br />메일이 없다면 스팸함도 확인해 주세요.</p>
            <button type="button" className="btn btn-ghost" onClick={() => setSent(false)}>다른 이메일 입력</button>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <FormGroup label="가입 이메일" htmlFor="reset-email" hint="계정 존재 여부와 관계없이 동일한 안내가 표시됩니다." required>
              <TextField id="reset-email" type="email" placeholder="you@example.com" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus />
            </FormGroup>
            {error ? <p className="form-error" role="alert">{error}</p> : null}
            <button className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>{submitting ? '메일 보내는 중…' : '재설정 링크 받기'}</button>
          </form>
        )}

        <p className="auth-footer-link"><Link className="auth-link-btn" href="/login">로그인으로 돌아가기</Link></p>
      </div>
    </div>
  );
}
