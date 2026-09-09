'use client';

import { useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { DataTable, type Column } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { StatusPill } from '@/components/ui/Tag';
import { TodoCreateModal } from './TodoCreateModal';
import { fetchTodos } from '@/lib/api';
import { GOAL_STATUS_LABELS } from '@/lib/constants';
import type { TodoItem } from '@/lib/types';

/** 한 쪽에 보여줄 건수. 서버에 그대로 넘기는 값이라 화면과 요청이 어긋나지 않는다 */
export const TODO_PAGE_SIZE = 5;

/**
 * 개인 TODO 목록 — 열면 개인용 팀 스페이스로 이동한다.
 *
 * 서버가 페이지 단위로 주므로(GET /todos/me) 쪽을 넘길 때마다 다시 읽는다.
 * 예전에는 첫 응답만 받아 화면에서 잘랐는데, 서버 기본값이 20건이라 21번째부터는 아예 안 보였다.
 * Pagination 은 1부터 세고 서버는 0부터라 요청할 때만 한 칸 내린다.
 */
export function TodoTable({
  todos: initial,
  totalPages: initialTotalPages,
}: {
  todos: TodoItem[];
  totalPages: number;
}) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [totalPages, setTotalPages] = useState(initialTotalPages);
  const [page, setPage] = useState(1);
  const [createOpen, setCreateOpen] = useState(false);
  // 빠르게 여러 쪽을 누르면 응답이 뒤섞여 온다. 마지막으로 요청한 쪽만 화면에 반영한다
  const latestRequest = useRef(1);

  const goToPage = (next: number) => {
    setPage(next);
    latestRequest.current = next;
    fetchTodos({ page: next - 1, size: TODO_PAGE_SIZE })
      .then((result) => {
        if (latestRequest.current !== next) return;
        setRows(result.items);
        setTotalPages(result.totalPages);
      })
      // 못 읽으면 지금 화면을 그대로 둔다 - 목록이 비어 사라지는 것보다 낫다
      .catch(() => undefined);
  };

  const columns: Column<TodoItem>[] = [
    {
      key: 'no',
      header: '번호',
      width: '3.25rem',
      render: (_row, index) => (page - 1) * TODO_PAGE_SIZE + index + 1,
    },
    { key: 'title', header: '제목', render: (row) => row.title },
    { key: 'created', header: '생성일', width: '6.875rem', render: (row) => row.createdAt },
    { key: 'category', header: '유형', width: '5.75rem', render: (row) => row.category },
    {
      key: 'progress',
      header: '진행률',
      width: '11.875rem',
      render: (row) => <ProgressBar done={row.doneCount} total={row.totalCount} />,
    },
    {
      key: 'status',
      header: '상태',
      width: '6rem',
      render: (row) => (
        <StatusPill tone={row.status === 'IN_PROGRESS' ? 'live' : 'default'}>
          {GOAL_STATUS_LABELS[row.status]}
        </StatusPill>
      ),
    },
    {
      key: 'open',
      header: '열기',
      width: '5.25rem',
      render: (row) => (
        <button
          type="button"
          className="tbl-open-btn"
          onClick={() => router.push(`/mypage/todo/${row.id}`)}
        >
          열기
        </button>
      ),
    },
  ];

  return (
    <section className="block">
      <div className="todo-head-row">
        <div>
          <h3 className="block-title" style={{ marginBottom: '0.25rem' }}>
            개인 TODO
          </h3>
          <p style={{ fontSize: '.84rem', color: 'var(--text-dim)' }}>
            팀 없이 혼자 관리하는 목록이에요. 성취(Goal)의 체크리스트 항목으로 저장됩니다.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
          새 TODO 만들기
        </button>
      </div>

      <DataTable columns={columns} rows={rows} rowKey={(row) => row.id} />
      <Pagination page={page} totalPages={totalPages} onChange={goToPage} />

      <TodoCreateModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(todo) => {
          // 새 TODO 는 최신순 첫 쪽에 들어간다. 어차피 바로 이동하므로 목록은 첫 쪽만 맞춰 둔다
          setRows((prev) => [todo, ...prev].slice(0, TODO_PAGE_SIZE));
          setPage(1);
          setCreateOpen(false);
          router.push(`/mypage/todo/${todo.id}`);
        }}
      />
    </section>
  );
}
