'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { useConfirm } from './ConfirmDialog';

interface DeleteButtonProps {
  /** 버튼에 쓸 문구 (예: '파티 삭제') */
  label: string;
  /** 확인창 문구 */
  confirmTitle: string;
  confirmDescription: string;
  onDelete: () => Promise<void>;
  /** 삭제 후 이동할 목록 경로 */
  redirectTo: string;
  /** 사이드카드 안에서 폭을 꽉 채워 쓸 때 */
  block?: boolean;
  className?: string;
}

/**
 * 상세 페이지의 삭제 버튼 — 파티 · 대회 · 전시가 같은 모양과 흐름을 쓴다.
 *
 * 삭제는 되돌릴 수 없으므로 확인창을 한 번 거치고, 성공하면 목록으로 보낸다.
 * (지운 상세에 그대로 머무르면 없는 글을 보고 있게 된다)
 * 실패하면 그 자리에 이유를 띄우고 페이지는 그대로 둔다.
 */
export function DeleteButton({
  label,
  confirmTitle,
  confirmDescription,
  onDelete,
  redirectTo,
  block,
  className,
}: DeleteButtonProps) {
  const router = useRouter();
  const { confirm, dialog } = useConfirm();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');

  const run = async () => {
    const ok = await confirm({
      title: confirmTitle,
      description: confirmDescription,
      confirmLabel: '삭제',
    });
    if (!ok) return;

    setPending(true);
    setError('');
    try {
      await onDelete();
      router.push(redirectTo);
      router.refresh();
    } catch {
      setError('삭제하지 못했어요. 잠시 후 다시 시도해 주세요.');
      setPending(false);
    }
  };

  return (
    <>
      <button
        type="button"
        className={[className ?? 'btn btn-ghost', 'is-danger'].join(' ')}
        style={block ? { width: '100%' } : undefined}
        onClick={run}
        disabled={pending}
      >
        <Icon name="i-trash" />
        {pending ? '삭제 중…' : label}
      </button>
      {error ? <p className="tool-error-note">{error}</p> : null}
      {dialog}
    </>
  );
}
