'use client';

interface FilterChipsProps {
  label: string;
  options: readonly string[];
  value: string;
  onChange: (value: string) => void;
}

/** 게시판 상단의 분야 필터 칩 */
export function FilterChips({ label, options, value, onChange }: FilterChipsProps) {
  return (
    <div className="filter-group">
      <span className="filter-label">{label}</span>
      {options.map((option) => (
        <button
          key={option}
          type="button"
          className={option === value ? 'filter-chip is-active' : 'filter-chip'}
          onClick={() => onChange(option)}
        >
          {option}
        </button>
      ))}
    </div>
  );
}

export function FilterBlock({ children }: { children: React.ReactNode }) {
  return <div className="filter-block">{children}</div>;
}
