'use client';

import { useMemo, useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { FormGroup, TextField } from '@/components/ui/Field';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import {
  completeSoloItem,
  createSoloItem,
  deleteSoloItem,
  updateSoloItem,
} from '@/lib/api';
import type { ChecklistItem } from '@/lib/types';
import { isEnterCommit } from '@/lib/ime';

interface ChecklistProps {
  /** 개인 TODO id */
  todoId: string;
  items: ChecklistItem[];
  ownerName: string;
}

/**
 * 개인 TODO 체크리스트 — 백엔드 PersonalTodoItem(`/api/v1/todos/{id}/items/**`).
 * 혼자 관리하는 목록이라 담당자·동료 승인 없이 바로 완료 처리된다.
 */
export function Checklist({ todoId, items: initialItems, ownerName }: ChecklistProps) {
  const { confirm, dialog } = useConfirm();
  const [items, setItems] = useState<ChecklistItem[]>(initialItems);
  const [content, setContent] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  // 저장이 끝나기 전에 한 번 더 누르면 같은 내용이 두 건 등록된다. 그 사이를 막는다.
  const [submitting, setSubmitting] = useState(false);

  const done = useMemo(() => items.filter((item) => item.state === 'done').length, [items]);

  const submitForm = async () => {
    const text = content.trim();
    if (!text || submitting) return;

    setSubmitting(true);
    try {
      if (editingId) {
        setItems((prev) =>
          prev.map((item) => (item.id === editingId ? { ...item, content: text } : item)),
        );
        await updateSoloItem(todoId, editingId, text);
        setEditingId(null);
      } else {
        // id 는 서버가 정한다. 화면에서 만들면 같은 밀리초에 두 건이 겹쳐 key 가 중복된다.
        const created = await createSoloItem(todoId, text);
        setItems((prev) => [...prev, created]);
      }
      setContent('');
    } finally {
      setSubmitting(false);
    }
  };

  const startEdit = (item: ChecklistItem) => {
    setEditingId(item.id);
    setContent(item.content);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setContent('');
  };

  const remove = async (id: string) => {
    const target = items.find((item) => item.id === id);
    const ok = await confirm({
      title: '할 일을 삭제할까요?',
      description: `'${target?.content ?? ''}' 항목이 사라져요. 되돌릴 수 없습니다.`,
    });
    if (!ok) return;

    setItems((prev) => prev.filter((item) => item.id !== id));
    if (editingId === id) cancelEdit();
    await deleteSoloItem(todoId, id);
  };

  const complete = async (item: ChecklistItem) => {
    setItems((prev) => prev.map((row) => (row.id === item.id ? { ...row, state: 'done' } : row)));
    await completeSoloItem(todoId, item.id);
  };

  return (
    <>
      <ProgressBar done={done} total={items.length} />

      <p className="checklist-quorum">
        개인 목록이라 승인 절차 없이 바로 완료 처리돼요. 담당자는 생성자인 <b>{ownerName}</b>로
        고정됩니다.
      </p>

      <div className="checklist">
        {items.map((item) => (
          <div key={item.id} className="checklist-item" data-state={item.state}>
            <span className="check-mark">
              <Icon name="i-check" />
            </span>
            <p className="content">{item.content}</p>
            {item.state !== 'done' ? (
              <>
                <button type="button" className="ci-edit-btn" onClick={() => startEdit(item)}>
                  <Icon name="i-pencil" />
                  수정
                </button>
                <button type="button" className="ci-del-btn" onClick={() => remove(item.id)}>
                  <Icon name="i-trash" />
                  삭제
                </button>
              </>
            ) : null}

            <span className="item-action">
              {item.state === 'done' ? (
                <span className="wait-note">완료</span>
              ) : (
                <button type="button" className="btn-mini" onClick={() => complete(item)}>
                  완료
                </button>
              )}
            </span>
          </div>
        ))}
      </div>

      {dialog}

      <div className="checklist-create">
        <div className="cc-head">
          <h4 className="cc-title">{editingId ? '할 일 수정' : '할 일 추가'}</h4>
          {editingId ? <span className="cc-mode-pill">EDIT</span> : null}
        </div>
        {/*
          담당자 칸은 숨겨 뒀다. 개인 TODO 는 담당자가 생성자로 고정이라 고를 게 없어서다.
          나중에 그룹(팀) 목록으로 넓히면 아래 주석의 FormGroup 을 되살리고
          cc-row 의 gridTemplateColumns 를 '2fr 1fr' 로 돌리면 된다.
        */}
        <div className="cc-row" style={{ gridTemplateColumns: '1fr' }}>
          <FormGroup label="할 일" htmlFor="checklistInput">
            <TextField
              id="checklistInput"
              placeholder="예: 2024년 기출 3회분 풀이"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              onKeyDown={(event) => {
                if (isEnterCommit(event)) {
                  event.preventDefault();
                  submitForm();
                }
              }}
            />
          </FormGroup>
          {/* <FormGroup label="담당자">
            <TextField value={`${ownerName} (생성자 고정)`} disabled />
          </FormGroup> */}
        </div>
        <div className="cc-foot">
          {editingId ? (
            <button type="button" className="btn-cancel" onClick={cancelEdit}>
              취소
            </button>
          ) : null}
          <button
            type="button"
            className="btn btn-primary"
            onClick={submitForm}
            disabled={submitting}
          >
            {editingId ? '수정 저장' : '할 일 추가'}
          </button>
        </div>
      </div>
    </>
  );
}
