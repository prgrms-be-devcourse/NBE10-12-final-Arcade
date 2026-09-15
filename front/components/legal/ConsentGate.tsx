'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import type { LegalDocument } from '@/lib/legal';

/**
 * 끝에 닿았다고 볼 여유. 관성 스크롤과 소수점 오차 때문에 정확히 0 이 되지 않는다.
 *
 * px 이 아니라 rem 으로 잡는 이유는 사용자가 글꼴을 키우면 줄 높이도 같이 커지기 때문이다 -
 * 고정 px 이면 큰 글꼴에서 판정이 뻑뻑해진다. 이 프로젝트 스타일도 전부 rem 이다.
 */
const BOTTOM_THRESHOLD_REM = 0.5;

/** rem 을 지금 루트 글꼴 기준 px 로 환산한다. 스크롤 값이 px 단위라 비교 전에 맞춰야 한다 */
function remToPx(rem: number): number {
  const rootFontSize = parseFloat(getComputedStyle(document.documentElement).fontSize);
  return rem * (Number.isFinite(rootFontSize) ? rootFontSize : 16);
}

interface ConsentGateProps {
  document: LegalDocument;
  checked: boolean;
  onChange: (checked: boolean) => void;
  required?: boolean;
}

/**
 * 약관 전문을 끝까지 내려야 동의 체크가 열리는 블록 (은행 앱 방식).
 *
 * "읽었는지" 는 법이 요구하는 조건이 아니다 - 요구되는 건 동의를 받았고 그 사실을 증빙할 수 있는가다.
 * 스크롤 게이트는 분쟁 시 '고지했다' 를 보강하는 UX 장치라, **못 하는 사람이 생기지 않는 쪽**이 우선이다.
 * 그래서 아래 세 가지를 함께 둔다.
 *
 *  1. 본문이 짧거나 화면이 커서 스크롤이 아예 안 생기면 onScroll 이 한 번도 불리지 않는다.
 *     그대로 두면 체크를 영영 못 한다 - 마운트 시점에 먼저 판정한다.
 *  2. 확대/축소·폰트 변경으로 나중에 같은 상황이 될 수 있어 ResizeObserver 로 다시 판정한다.
 *  3. 키보드·스크린리더 사용자는 스크롤 이벤트 없이 끝에 닿을 수 있어,
 *     본문을 포커스 가능하게 두고 비활성 사유를 aria-describedby 로 알린다.
 */
export function ConsentGate({ document, checked, onChange, required = true }: ConsentGateProps) {
  const bodyRef = useRef<HTMLDivElement>(null);
  const [readToEnd, setReadToEnd] = useState(false);
  const hintId = `consent-hint-${document.key}`;

  const check = useCallback((element: HTMLDivElement) => {
    const reachedBottom =
      element.scrollHeight - element.scrollTop - element.clientHeight <
      remToPx(BOTTOM_THRESHOLD_REM);
    // 한 번 열리면 다시 닫지 않는다 - 위로 올렸다고 안 읽은 게 되지는 않는다
    if (reachedBottom) setReadToEnd(true);
  }, []);

  useEffect(() => {
    const element = bodyRef.current;
    if (!element) return;

    check(element);

    const observer = new ResizeObserver(() => check(element));
    observer.observe(element);

    return () => observer.disconnect();
  }, [check]);

  return (
    <div className="consent-gate">
      <div className="consent-gate-head">
        <span className={required ? 'req' : undefined}>{required ? '[필수]' : '[선택]'}</span>
        <strong>{document.title}</strong>
        {/* 가입 흐름을 벗어나지 않고도 전문을 크게 볼 수 있게 한다 */}
        <Link className="consent-link" href={`/legal/${document.key}`} target="_blank">
          전문 보기
        </Link>
      </div>

      <div
        ref={bodyRef}
        className="consent-gate-body"
        // 마우스 없이도 본문을 훑을 수 있어야 한다
        tabIndex={0}
        role="region"
        aria-label={`${document.title} 전문`}
        onScroll={(event) => check(event.currentTarget)}
      >
        {document.body}
      </div>

      <label className="consent-row">
        <input
          type="checkbox"
          className={required ? 'consent-required' : undefined}
          checked={checked}
          disabled={!readToEnd}
          aria-describedby={readToEnd ? undefined : hintId}
          onChange={(event) => onChange(event.target.checked)}
        />
        <span>{document.label}</span>
      </label>

      {readToEnd ? null : (
        <p id={hintId} className="form-hint">
          전문을 끝까지 확인하면 동의할 수 있어요.
        </p>
      )}
    </div>
  );
}
