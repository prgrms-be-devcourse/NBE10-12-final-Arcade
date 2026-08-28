'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AuthLogo } from './AuthLogo';
import { SocialButtons } from './SocialButtons';
import { FormGroup, TextField } from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { login } from '@/lib/api';
import type { MemberType } from '@/lib/api/auth';

const MEMBER_TYPES = ['일반', '주최측'] as const;

export function LoginForm() {
  const router = useRouter();
  const [memberType, setMemberType] = useState<MemberType>('일반');
  const [email, setEmail] = useState('haneul@crewon.dev');
  const [password, setPassword] = useState('password123');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    setSubmitting(true);
    try {
      const user = await login({ email, password, memberType });
      router.push(user.role === 'HOST' ? '/host' : '/');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <AuthLogo />
        <h1 className="auth-title">로그인</h1>
        <p className="auth-subtitle">성취로 증명하는 팀 매칭, 크루온에 다시 오신 걸 환영해요.</p>

        <FormGroup label="회원 유형" className="" >
          <RadioChipGroup
            options={MEMBER_TYPES}
            value={memberType}
            onChange={(value) => setMemberType(value as MemberType)}
          />
        </FormGroup>

        <SocialButtons
          suffix="계속하기"
          hidden={memberType === '주최측'}
          onSuccess={() => router.push('/')}
        />
        {memberType === '주최측' ? (
          <p className="social-locked-note">
            주최측 계정은 소셜 로그인을 지원하지 않아요. 가입 시 사용한 담당자 이메일로 로그인해
            주세요.
          </p>
        ) : null}

        {memberType === '주최측' ? null : (
          <div className="auth-divider">
            <span>또는 이메일로 로그인</span>
          </div>
        )}

        <form className="auth-form" onSubmit={(event) => event.preventDefault()}>
          <FormGroup label="이메일">
            <TextField
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </FormGroup>
          <FormGroup label="비밀번호">
            <TextField
              type="password"
              placeholder="비밀번호 입력"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </FormGroup>
          <button
            type="button"
            className="btn btn-primary"
            style={{ width: '100%' }}
            onClick={submit}
            disabled={submitting}
          >
            {submitting ? '로그인 중…' : '로그인'}
          </button>
        </form>

        <p className="auth-footer-link">
          계정이 없으신가요?{' '}
          <Link className="auth-link-btn" href="/signup">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
