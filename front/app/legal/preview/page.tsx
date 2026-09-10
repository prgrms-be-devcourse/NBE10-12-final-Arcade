'use client';

import { useState } from 'react';
import { OnboardingModal, type OnboardingResult } from '@/components/auth/OnboardingModal';

/**
 * GitHub 온보딩 모달 미리보기.
 *
 * 실제 노출 조건(GET /members/me 의 privacyAgreedAt 이 null)은 서버가 그 필드를 내려줘야 판정할 수 있어
 * 아직 붙이지 못한다. 화면만 먼저 확인하려고 둔 임시 경로다 -
 * 온보딩을 실제 흐름에 연결할 때(docs 8장 B2) 이 파일은 지운다.
 */
export default function OnboardingPreviewPage() {
  const [open, setOpen] = useState(true);
  const [saved, setSaved] = useState<OnboardingResult | null>(null);

  return (
    <main>
      <div className="legal-wrap">
        <h2>온보딩 모달 미리보기</h2>
        <p className="form-hint">
          서버 연결 전 화면 확인용입니다. 실제로는 GitHub 가입 직후 자동으로 뜹니다.
        </p>

        {saved ? (
          <p className="form-hint">
            받은 값 — 닉네임: {saved.nickname} / 포지션: {saved.position}
          </p>
        ) : null}

        <button type="button" className="btn btn-primary" onClick={() => setOpen(true)}>
          다시 열기
        </button>
      </div>

      <OnboardingModal
        open={open}
        defaultNickname="octocat"
        onSubmit={(result) => {
          setSaved(result);
          setOpen(false);
        }}
      />
    </main>
  );
}
