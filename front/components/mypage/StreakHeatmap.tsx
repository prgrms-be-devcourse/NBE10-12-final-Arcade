/** 최근 8주 활동 히트맵 — 값은 0~3 레벨 배열 */
const DEFAULT_LEVELS = [
  0, 1, 0, 2, 0, 0, 1, 1, 0, 1, 2, 0, 1, 0, 0, 2, 2, 0, 1, 0, 1, 1, 0, 3, 2, 1, 0, 0, 0, 1, 2, 3, 2,
  1, 0, 2, 3, 3, 2, 3, 2, 3, 3, 3, 2, 3, 3, 2, 3, 3, 2, 3, 3, 2, 3, 3,
];

export function StreakHeatmap({ levels = DEFAULT_LEVELS }: { levels?: number[] }) {
  return (
    <div className="streak-heatmap" aria-label="최근 8주 활동 기록">
      {levels.map((level, index) => (
        <span key={index} className={level > 0 ? `lv${level}` : undefined} />
      ))}
    </div>
  );
}
