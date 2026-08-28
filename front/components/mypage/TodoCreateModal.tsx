'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, Options, SelectField, TextAreaField, TextField } from '@/components/ui/Field';
import { createTodo } from '@/lib/api';
import type { TodoItem } from '@/lib/types';

const TODO_CATEGORIES = ['학습', '사이드', '루틴', '기록', '기타'] as const;

interface TodoCreateModalProps {
  open: boolean;
  onClose: () => void;
  /** 생성 후 '만들고 열기' — 목록에 추가하고 개인 스페이스로 이동한다 */
  onCreated: (todo: TodoItem) => void;
}

export function TodoCreateModal({ open, onClose, onCreated }: TodoCreateModalProps) {
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState<string>(TODO_CATEGORIES[0]);
  const [memo, setMemo] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setTitle('');
    setCategory(TODO_CATEGORIES[0]);
    setMemo('');
    setError(null);
  };

  const submit = async () => {
    if (!title.trim()) {
      setError('제목을 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    try {
      const created = await createTodo({ title: title.trim(), category, memo });
      onCreated(created);
      reset();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      title="새 개인 TODO 만들기"
      description="혼자 관리하는 목록이라 팀원 초대나 승인 절차가 없어요."
      confirmLabel={submitting ? '만드는 중…' : '만들고 열기'}
      cancelLabel="닫기"
      onConfirm={submit}
      onClose={() => {
        reset();
        onClose();
      }}
    >
      <FormGroup label="제목">
        <TextField
          placeholder="예: 정보처리기사 실기 준비"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
            setError(null);
          }}
          autoFocus
        />
      </FormGroup>

      <FormGroup label="유형">
        <SelectField value={category} onChange={(event) => setCategory(event.target.value)}>
          <Options values={TODO_CATEGORIES} />
        </SelectField>
      </FormGroup>

      <FormGroup label="개인 메모 (선택)">
        <TextAreaField
          placeholder="나만 보는 메모예요."
          value={memo}
          onChange={(event) => setMemo(event.target.value)}
        />
      </FormGroup>

      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
