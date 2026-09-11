'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, TextField } from '@/components/ui/Field';
import { ApiError, changePassword } from '@/lib/api';
import { passwordPolicyError } from '@/lib/password';

export function PasswordChangeModal({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const submit = async () => {
    if (submitting) return;
    if (!currentPassword) return setError('현재 비밀번호를 입력해 주세요.');
    const policyError = passwordPolicyError(newPassword);
    if (policyError) return setError(policyError);
    if (newPassword !== newPasswordConfirm) return setError('새 비밀번호가 일치하지 않아요.');

    setError('');
    setSubmitting(true);
    try {
      await changePassword({ currentPassword, newPassword, newPasswordConfirm });
      setDone(true);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : '비밀번호를 변경하지 못했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  const finish = () => {
    router.replace('/login');
    router.refresh();
  };

  return (
    <Modal
      open
      title="비밀번호 변경"
      description={done ? '기존 로그인 세션이 모두 종료됐어요.' : '본인 확인을 위해 현재 비밀번호가 필요해요.'}
      confirmLabel={done ? undefined : submitting ? '변경 중…' : '비밀번호 변경'}
      cancelLabel={done ? '로그인하러 가기' : '취소'}
      onConfirm={submit}
      onClose={done ? finish : onClose}
      dismissible={!submitting}
      footNote={error ? <span className="form-field-error" role="alert">{error}</span> : undefined}
    >
      {done ? (
        <div className="password-change-success" role="status">새 비밀번호로 다시 로그인해 주세요.</div>
      ) : (
        <>
          <FormGroup label="현재 비밀번호" htmlFor="current-password" required><TextField id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} autoFocus /></FormGroup>
          <FormGroup label="새 비밀번호" htmlFor="change-new-password" hint="8~64자, 문자·숫자·특수문자 포함, 공백 제외" required><TextField id="change-new-password" type="password" minLength={8} maxLength={64} autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></FormGroup>
          <FormGroup label="새 비밀번호 확인" htmlFor="change-new-password-confirm" required><TextField id="change-new-password-confirm" type="password" minLength={8} maxLength={64} autoComplete="new-password" value={newPasswordConfirm} onChange={(event) => setNewPasswordConfirm(event.target.value)} /></FormGroup>
        </>
      )}
    </Modal>
  );
}
