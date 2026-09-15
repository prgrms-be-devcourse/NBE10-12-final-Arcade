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
        <button type="button" className="page-btn" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        이전
      </button>
      {Array.from({ length: totalPages }, (_, index) => index + 1).map((num) => (
        <button
          key={num}
          type="button"
          className={num === page ? 'page-btn is-active' : 'page-btn'}
          aria-current={num === page ? 'page' : undefined}
          onClick={() => onChange(num)}
        >
          {num}
        </button>
      ))}
      <button type="button" className="page-btn" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        다음
      </button>
    </div>
  );
}
