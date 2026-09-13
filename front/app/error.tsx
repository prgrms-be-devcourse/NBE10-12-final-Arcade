'use client';

import Link from 'next/link';
import { useEffect } from 'react';

export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="system-state" role="alert">
      <section className="system-state-card system-state-card--error">
        <div className="system-state-console" aria-hidden="true">
          <span className="system-state-light system-state-light--error" />
          <span className="system-state-screen">
            <span className="system-state-screen-text">ERROR</span>
            <span className="system-state-code">500</span>
          </span>
        </div>
        <p className="system-state-kicker system-state-kicker--error">SYSTEM MESSAGE</p>
        <h1>앗, 문제가 발생했어요</h1>
        <p className="system-state-description">
          크루온 서버와 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
        <div className="system-state-actions">
          <button type="button" className="btn btn-primary" onClick={() => reset()}>
            다시 시도
          </button>
          <Link href="/" className="btn btn-ghost">
            홈으로 이동
          </Link>
        </div>
        <p className="system-state-hint">문제가 계속되면 잠시 후 다시 접속해 주세요.</p>
      </section>
    </main>
  );
}
