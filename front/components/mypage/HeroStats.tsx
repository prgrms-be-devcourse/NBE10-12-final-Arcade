import { StreakHeatmap } from './StreakHeatmap';

interface HeroStatsProps {
  streakDays: number;
}

/**
 * 연속 활동 히트맵.
 *
 * 배지 칸은 잠시 숨겨 뒀다 — 서버에 배지 도메인이 없어 항상 빈 칸으로 보였다
 * (docs/마이페이지-요약API_백엔드_요청.md ①).
 *
 * 되살리려면 아래 세 가지를 되돌리면 된다.
 *   1. 주석 처리한 '배지' hero-stats-col 블록과 badges prop · BadgeItem · Icon import
 *   2. section 의 gridTemplateColumns 인라인 값 (2단으로 돌아간다)
 *   3. MypageView 의 <HeroStats ... badges={profile.badges} />
 */
export function HeroStats({ streakDays }: HeroStatsProps) {
  const remaining = Math.max(0, 30 - streakDays);

  return (
    // 배지 칸을 숨긴 동안에는 한 칸만 쓴다. 2단 그대로 두면 오른쪽이 빈 채로 남는다.
    <section className="hero-stats" data-reveal style={{ gridTemplateColumns: '1fr' }}>
      <div className="hero-stats-col">
        <h4>연속 활동</h4>
        <StreakHeatmap />
        <p className="streak-caption">
          {streakDays}일 연속 기록 중 · 스트릭 배지(30일)까지 {remaining}일 남음
        </p>
      </div>
      {/*
      <div className="hero-stats-col">
        <h4>배지</h4>
        <div className="badge-grid">
          {badges.map((badge) => (
            <div key={badge.id} className={badge.earned ? 'badge-tile earned' : 'badge-tile'}>
              <Icon name={badge.icon as IconName} />
              <span>{badge.label}</span>
            </div>
          ))}
        </div>
      </div>
      */}
    </section>
  );
}
