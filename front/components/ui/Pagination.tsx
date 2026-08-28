'use client';

interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button type="button" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        이전
      </button>
      {Array.from({ length: totalPages }, (_, index) => index + 1).map((num) => (
        <button
          key={num}
          type="button"
          className={num === page ? 'is-active' : undefined}
          onClick={() => onChange(num)}
        >
          {num}
        </button>
      ))}
      <button type="button" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        다음
      </button>
    </div>
  );
}
