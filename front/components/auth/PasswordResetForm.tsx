'use client';

import { useEffect, useState, type FormEvent } from 'react';
import Link from 'next/link';
import { AuthLogo } from './AuthLogo';
import { FormGroup, TextField } from '@/components/ui/Field';
import { ApiError, resetPassword, validatePasswordResetToken } from '@/lib/api';
import { passwordPolicyError } from '@/lib/password';

type TokenState = 'checking' | 'valid' | 'invalid' | 'done';

export function PasswordResetForm({ token }: { token: string }) {
  const [tokenState, setTokenState] = useState<TokenState>('checking');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const valid = token ? await validatePasswordResetToken(token) : false;
        if (active) setTokenState(valid ? 'valid' : 'invalid');
      } catch {
        if (active) setTokenState('invalid');
      }
    })();
    return () => { active = false; };
  }, [token]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting || tokenState !== 'valid') return;
    const policyError = passwordPolicyError(password);
    if (policyError) return setError(policyError);
    if (password !== passwordConfirm) return setError('새 비밀번호가 일치하지 않아요.');

    setError('');
    setSubmitting(true);
    try {
      await resetPassword({ token, newPassword: password, newPasswordConfirm: passwordConfirm });
      setTokenState('done');
    } catch (cause) {
      if (cause instanceof ApiError && cause.resultCode === '400-3') setTokenState('invalid');
      else setError(cause instanceof ApiError ? cause.message : '비밀번호를 재설정하지 못했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <AuthLogo />
        <h1 className="auth-title">새 비밀번호 설정</h1>
        {tokenState === 'checking' ? <div className="password-reset-state" role="status"><span className="password-reset-spinner" aria-hidden="true" /><p>재설정 링크를 확인하고 있어요…</p></div> : null}
        {tokenState === 'invalid' ? (
          <div className="password-reset-done" role="alert"><span aria-hidden="true">!</span><h2>사용할 수 없는 링크예요</h2><p>링크가 만료됐거나 이미 사용됐어요. 새 링크를 받아 주세요.</p><Link className="btn btn-primary" href="/forgot-password">재설정 링크 다시 받기</Link></div>
        ) : null}
        {tokenState === 'valid' ? (
          <><p className="auth-subtitle">기존 비밀번호와 다른 새 비밀번호를 입력해 주세요.</p><form className="auth-form" onSubmit={submit}>
            <FormGroup label="새 비밀번호" htmlFor="new-password" hint="8~64자, 문자·숫자·특수문자 포함, 공백 제외" required><TextField id="new-password" type="password" minLength={8} maxLength={64} autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} required autoFocus /></FormGroup>
            <FormGroup label="새 비밀번호 확인" htmlFor="new-password-confirm" required><TextField id="new-password-confirm" type="password" minLength={8} maxLength={64} autoComplete="new-password" value={passwordConfirm} onChange={(event) => setPasswordConfirm(event.target.value)} required /></FormGroup>
            {error ? <p className="form-error" role="alert">{error}</p> : null}<button className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>{submitting ? '변경하는 중…' : '비밀번호 재설정'}</button>
          </form></>
        ) : null}
        {tokenState === 'done' ? <div className="password-reset-done" role="status"><span aria-hidden="true">✓</span><h2>비밀번호를 변경했어요</h2><p>보안을 위해 기존 로그인 세션이 종료됐어요. 새 비밀번호로 로그인해 주세요.</p><Link className="btn btn-primary" href="/login">로그인하러 가기</Link></div> : null}
      </div>
    </div>
  );
}
