'use client';

import { useState } from 'react';
import { ConsentGate } from '@/components/legal/ConsentGate';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, SelectField, TextField } from '@/components/ui/Field';
import { LEGAL_DOCUMENTS } from '@/lib/legal';
import { POSITION_LABELS, POSITION_TYPES } from '@/lib/constants';
import type { PositionType } from '@/lib/types';

export interface OnboardingResult {
  nickname: string;
  position: PositionType;
}

interface OnboardingModalProps {
  open: boolean;
  /** GitHub 이 준 이름. 닉네임 입력의 초기값으로만 쓴다 */
  defaultNickname?: string;
  onSubmit: (result: OnboardingResult) => Promise<void> | void;
}

/**
 * GitHub 으로 가입한 회원의 최초 진입 화면 (기획서 2.1 · 3.4).
 *
 * GitHub 로그인은 폼이 없어 가입 과정에서 약관 동의를 받을 자리가 없다.
 * 그래서 콜백으로 돌아온 뒤 이 화면을 띄우고, **통과하기 전에는 서비스를 쓸 수 없게** 한다
 * (서버도 필터로 같은 상태를 강제한다 - docs/약관동의-설계_개인정보보호법_ISMS.md 5장).
 *
 * 닫을 수 없는 모달이라 `dismissible={false}` 다. 닫기·ESC·배경 클릭이 모두 막힌다.
 *
 * 닉네임·포지션을 함께 받는 이유는 GitHub 가입자가 이 둘도 비어 있기 때문이다 -
 * 어차피 한 번 붙잡는 화면이라 나눠 묻지 않는다.
 */
export function OnboardingModal({ open, defaultNickname = '', onSubmit }: OnboardingModalProps) {
  const [agreements, setAgreements] = useState<string[]>([]);
  const [nickname, setNickname] = useState(defaultNickname);
  const [position, setPosition] = useState<PositionType>('BACK');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const toggle = (key: string) =>
    setAgreements((prev) =>
      prev.includes(key) ? prev.filter((value) => value !== key) : [...prev, key],
    );

  // LEGAL_DOCUMENTS 는 둘 다 필수다. 선택 동의가 생기면 required 플래그로 갈라야 한다.
  const agreedAll = LEGAL_DOCUMENTS.every((document) => agreements.includes(document.key));

  const submit = async () => {
    if (!agreedAll) return setError('필수 약관에 모두 동의해 주세요.');
    if (!nickname.trim()) return setError('닉네임을 입력해 주세요.');

    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({ nickname: nickname.trim(), position });
    } catch {
      setError('저장하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      dismissible={false}
      title="시작하기 전에"
      description="약관에 동의하고 프로필을 설정하면 크루온을 이용할 수 있어요."
      confirmLabel={submitting ? '저장 중…' : '시작하기'}
      onConfirm={submit}
      // 본문은 스크롤되므로 오류를 그 안에 두면 위쪽을 보고 있을 때 안 보인다
      footNote={error}
      // dismissible={false} 라 불리지 않지만 Modal 의 필수 prop 이다
      onClose={() => {}}
    >
      <div className="consent-block">
        {LEGAL_DOCUMENTS.map((document) => (
          <ConsentGate
            key={document.key}
            document={document}
            checked={agreements.includes(document.key)}
            onChange={() => toggle(document.key)}
          />
        ))}
      </div>

      <div className="onboarding-fields">
        <FormGroup label="닉네임" required>
          <TextField
            placeholder="크루온에서 쓸 이름"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
          />
        </FormGroup>

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
      </div>

    </Modal>
  );
}
