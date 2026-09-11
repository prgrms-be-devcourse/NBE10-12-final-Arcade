import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  header: ReactNode;
  /** CSS 길이 문자열 (rem 권장) */
  width?: string;
  render: (row: T, index: number) => ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string;
  emptyMessage?: string;
  loading?: boolean;
  error?: string;
}

/** 마이페이지 TODO · 관리자 콘솔에서 공통으로 쓰는 표 */
export function DataTable<T>({ columns, rows, rowKey, emptyMessage, loading = false, error }: DataTableProps<T>) {
  return (
    <div className="data-table-wrap" tabIndex={0}>
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((column) => (
            <th key={column.key} style={column.width ? { width: column.width } : undefined}>
              {column.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {loading || error || rows.length === 0 ? (
          <tr>
            <td colSpan={columns.length} className="data-table-empty" role={error ? 'alert' : 'status'}>
              {loading ? '불러오는 중이에요…' : error ?? emptyMessage ?? '표시할 데이터가 없어요.'}
            </td>
          </tr>
        ) : (
          rows.map((row, index) => (
            <tr key={rowKey(row, index)}>
              {columns.map((column) => (
                <td key={column.key}>{column.render(row, index)}</td>
              ))}
            </tr>
          ))
        )}
      </tbody>
    </table>
    </div>
  );
}
