import { Icon, type IconName } from '@/components/icons/Icon';
import { StreakHeatmap } from './StreakHeatmap';
import type { BadgeItem } from '@/lib/types';

interface HeroStatsProps {
  streakDays: number;
  badges: BadgeItem[];
}

/** 연속 활동 히트맵 + 배지 */
export function HeroStats({ streakDays, badges }: HeroStatsProps) {
  const remaining = Math.max(0, 30 - streakDays);

  return (
    <section className="hero-stats" data-reveal>
      <div className="hero-stats-col">
        <h4>연속 활동</h4>
        <StreakHeatmap />
        <p className="streak-caption">
          {streakDays}일 연속 기록 중 · 스트릭 배지(30일)까지 {remaining}일 남음
        </p>
      </div>
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
    </section>
  );
}
