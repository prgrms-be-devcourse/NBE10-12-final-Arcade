interface ProgressBarProps {
  done: number;
  total: number;
}

/** 체크리스트 진행률 게이지 (HP 바) */
export function ProgressBar({ done, total }: ProgressBarProps) {
  const percent = total ? Math.round((done / total) * 100) : 0;
  return (
    <div className="checklist-summary">
      <div className="progress-track">
        <span className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
      <span className="count">
        {done}/{total}
      </span>
    </div>
  );
}
