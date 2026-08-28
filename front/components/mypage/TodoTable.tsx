'use client';

import { useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { DataTable, type Column } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { StatusPill } from '@/components/ui/Tag';
import { TodoCreateModal } from './TodoCreateModal';
import { GOAL_STATUS_LABELS } from '@/lib/constants';
import type { TodoItem } from '@/lib/types';

const PAGE_SIZE = 5;

/** 개인 TODO 목록 — 열면 개인용 팀 스페이스로 이동한다 */
export function TodoTable({ todos: initial }: { todos: TodoItem[] }) {
  const router = useRouter();
  const [todos, setTodos] = useState(initial);
  const [page, setPage] = useState(1);
  const [createOpen, setCreateOpen] = useState(false);

  const totalPages = Math.max(1, Math.ceil(todos.length / PAGE_SIZE));
  const rows = useMemo(
    () => todos.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [todos, page],
  );

  const columns: Column<TodoItem>[] = [
    {
      key: 'no',
      header: '번호',
      width: '3.25rem',
      render: (_row, index) => (page - 1) * PAGE_SIZE + index + 1,
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
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      <TodoCreateModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(todo) => {
          setTodos((prev) => [todo, ...prev]);
          setPage(1);
          setCreateOpen(false);
          router.push(`/mypage/todo/${todo.id}`);
        }}
      />
    </section>
  );
}
