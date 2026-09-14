export default function Loading() {
  return (
    <main className="system-state" aria-busy="true" aria-live="polite">
      <section className="system-state-card" aria-label="페이지를 불러오는 중">
        <div className="system-state-console" aria-hidden="true">
          <span className="system-state-light" />
          <span className="system-state-screen">
            <span className="system-state-screen-text">LOADING</span>
            <span className="system-state-dots">...</span>
          </span>
        </div>
        <p className="system-state-kicker">CREWON SYSTEM</p>
        <h1>게임 화면을 준비하고 있어요</h1>
        <p className="system-state-description">잠시만 기다려 주세요. 곧 크루온으로 이동합니다.</p>
        <div className="system-state-progress" aria-hidden="true">
          <span />
        </div>
      </section>
    </main>
  );
}
