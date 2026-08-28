'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AuthLogo } from './AuthLogo';
import { CompanySetup } from './CompanySetup';
import { EmailVerifyField } from './EmailVerifyField';
import { SocialButtons } from './SocialButtons';
import {
  FormGroup,
  FormRow,
  SelectField,
  TextField,
} from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { POSITION_LABELS, POSITION_TYPES } from '@/lib/constants';
import { signup } from '@/lib/api';
import type { MemberType } from '@/lib/api/auth';
import type { PositionType } from '@/lib/types';

const MEMBER_TYPES = ['일반', '주최측'] as const;
const CONSENTS = [
  { key: 'terms', label: '이용약관 동의', required: true },
  { key: 'privacy', label: '개인정보 수집 및 이용 동의', required: true },
  { key: 'marketing', label: '이벤트·마케팅 정보 수신 동의', required: false },
] as const;

export function SignupForm() {
  const router = useRouter();
  const [memberType, setMemberType] = useState<MemberType>('일반');
  const [realName, setRealName] = useState('');
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [position, setPosition] = useState<PositionType>('BACK');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [agreements, setAgreements] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const allChecked = agreements.length === CONSENTS.length;

  const toggleAll = () =>
    setAgreements(allChecked ? [] : CONSENTS.map((consent) => consent.key));

  const toggle = (key: string) =>
    setAgreements((prev) => (prev.includes(key) ? prev.filter((value) => value !== key) : [...prev, key]));

  const submit = async () => {
    if (password.length < 8) return setError('비밀번호는 8자 이상이어야 해요.');
    if (password !== passwordConfirm) return setError('비밀번호가 일치하지 않아요.');
    const missing = CONSENTS.filter(
      (consent) => consent.required && !agreements.includes(consent.key),
    );
    if (missing.length > 0) return setError('필수 약관에 모두 동의해 주세요.');

    setError(null);
    setSubmitting(true);
    try {
      const user = await signup({
        email,
        password,
        nickname: nickname || realName,
        position,
        agreements,
        memberType,
      });
      router.push(user.role === 'HOST' ? '/host' : '/');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell auth-shell--wide">
      <div className="auth-split">
        <aside className="auth-aside">
          <span className="aside-badge">JOIN CREWON</span>
          <h2>
            완료한 프로젝트가
            <br />
            <em>그대로 이력</em>이 됩니다
          </h2>
          <p className="lead">
            파티에서 맡은 체크리스트와 승인 기록이 자동으로 쌓여, 말이 아니라 기록으로 증명하는 팀
            매칭을 시작하세요.
          </p>
          <div className="auth-points">
            <div className="auth-point">
              <span className="num">01</span>
              <span className="txt">
                <b>자동기록 성취</b>완료한 체크리스트가 담당자·승인자와 함께 프로필에 남아요.
              </span>
            </div>
            <div className="auth-point">
              <span className="num">02</span>
              <span className="txt">
                <b>공모전 연동 파티</b>주최측이 직접 등록한 공모전으로 팀을 꾸릴 수 있어요.
              </span>
            </div>
            <div className="auth-point">
              <span className="num">03</span>
              <span className="txt">
                <b>전시관 공개</b>결과물을 전시하고 채용 담당자에게 쪽지를 받아보세요.
              </span>
            </div>
          </div>
        </aside>

        <div className="auth-card auth-card--wide">
          <AuthLogo />
          <h1 className="auth-title">회원가입</h1>
          <p className="auth-subtitle">
            완료한 프로젝트와 수상 이력이 자동으로 쌓이는 팀 매칭을 시작하세요.
          </p>

          <SocialButtons
            suffix="시작하기"
            hidden={memberType === '주최측'}
            onSuccess={() => router.push('/')}
          />
          {memberType === '주최측' ? (
            <p className="social-locked-note">
              주최측 계정은 소셜 가입을 지원하지 않아요. 회사 담당자 이메일로 가입해 주세요.
            </p>
          ) : null}

          {memberType === '주최측' ? null : (
            <div className="auth-divider">
              <span>또는 이메일로 가입</span>
            </div>
          )}

          <form className="auth-form" onSubmit={(event) => event.preventDefault()}>
            <FormGroup
              label="회원 유형"
              hint={
                memberType === '일반'
                  ? '성취 프로필을 쌓고 파티에 지원할 수 있어요.'
                  : '공모전을 등록하고 참가팀 현황을 관리할 수 있어요.'
              }
            >
              <RadioChipGroup
                options={MEMBER_TYPES}
                value={memberType}
                onChange={(value) => setMemberType(value as MemberType)}
              />
            </FormGroup>

            <FormRow>
              <FormGroup label="성명" htmlFor="signupRealName">
                <TextField
                  id="signupRealName"
                  placeholder="실명을 입력해 주세요"
                  value={realName}
                  onChange={(event) => setRealName(event.target.value)}
                />
              </FormGroup>
              <FormGroup label="닉네임" htmlFor="signupNickname">
                <TextField
                  id="signupNickname"
                  placeholder="파티에서 사용할 닉네임"
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                />
              </FormGroup>
            </FormRow>

            <EmailVerifyField
              label="이메일"
              placeholder="you@example.com"
              idleMessage="가입 확인 메일을 보내 본인 인증을 진행해요."
              value={email}
              onChange={setEmail}
            />

            {memberType === '일반' ? (
              <FormGroup label="대표 포지션">
                <SelectField
                  value={position}
                  onChange={(event) => setPosition(event.target.value as PositionType)}
                >
                  {POSITION_TYPES.map((type) => (
              <option key={type} value={type}>
                {POSITION_LABELS[type]}
              </option>
            ))}
                </SelectField>
              </FormGroup>
            ) : (
              <CompanySetup />
            )}

            <FormRow>
              <FormGroup label="비밀번호">
                <TextField
                  type="password"
                  placeholder="8자 이상"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </FormGroup>
              <FormGroup label="비밀번호 확인">
                <TextField
                  type="password"
                  placeholder="비밀번호 재입력"
                  value={passwordConfirm}
                  onChange={(event) => setPasswordConfirm(event.target.value)}
                />
              </FormGroup>
            </FormRow>

            {error ? <p className="form-error">{error}</p> : null}

            <div className="consent-block">
              <label className="consent-row consent-all">
                <input type="checkbox" checked={allChecked} onChange={toggleAll} />
                <span>전체 동의</span>
              </label>
              {CONSENTS.map((consent) => (
                <label key={consent.key} className="consent-row">
                  <input
                    type="checkbox"
                    className={consent.required ? 'consent-required' : undefined}
                    checked={agreements.includes(consent.key)}
                    onChange={() => toggle(consent.key)}
                  />
                  <span>
                    <span className={consent.required ? 'req' : undefined}>
                      {consent.required ? '[필수]' : '[선택]'}
                    </span>
                    {consent.label}
                  </span>
                </label>
              ))}
            </div>

            <button
              type="button"
              className="btn btn-primary"
              style={{ width: '100%' }}
              onClick={submit}
              disabled={submitting}
            >
              {submitting ? '가입 중…' : '가입하기'}
            </button>
          </form>

          <p className="auth-footer-link">
            이미 계정이 있으신가요?{' '}
            <Link className="auth-link-btn" href="/login">
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
