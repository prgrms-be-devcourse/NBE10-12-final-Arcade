'use client';

interface RadioChipGroupProps {
  options: readonly string[];
  value: string;
  onChange: (value: string) => void;
}

/** 목업의 .radio-chip-group — 단일 선택 칩 그룹 */
export function RadioChipGroup({ options, value, onChange }: RadioChipGroupProps) {
  return (
    <div className="radio-chip-group">
      {options.map((option) => (
        <button
          key={option}
          type="button"
          className={option === value ? 'radio-chip is-active' : 'radio-chip'}
          onClick={() => onChange(option)}
        >
          {option}
        </button>
      ))}
    </div>
  );
}
