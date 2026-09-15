'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, SelectField, TextAreaField, TextField } from '@/components/ui/Field';
import { createTodo } from '@/lib/api';
import { TODO_CATEGORY_LABELS } from '@/lib/constants';
import type { TodoItem } from '@/lib/types';

/**
 * 서버 TodoCategory 값을 그대로 보낸다 - 한글 문구를 보내면 400-2 로 거절당한다.
 * 화면에는 라벨을 보여주므로 Options 헬퍼(값=문구) 대신 직접 그린다.
 */
const TODO_CATEGORIES = Object.keys(TODO_CATEGORY_LABELS);

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
          {TODO_CATEGORIES.map((value) => (
            <option key={value} value={value}>
              {TODO_CATEGORY_LABELS[value]}
            </option>
          ))}
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
