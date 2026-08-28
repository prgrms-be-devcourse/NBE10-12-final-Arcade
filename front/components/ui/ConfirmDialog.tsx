'use client';

import { useCallback, useState } from 'react';
import { Modal } from './Modal';

interface ConfirmOptions {
  title: string;
  description?: string;
  confirmLabel?: string;
}

interface PendingConfirm extends ConfirmOptions {
  resolve: (ok: boolean) => void;
}

/**
 * 삭제처럼 되돌리기 어려운 동작 앞에 확인 단계를 두기 위한 훅.
 *
 *   const { confirm, dialog } = useConfirm();
 *   if (await confirm({ title: '...' })) { ... }
 *   return <>{dialog}</>;
 */
export function useConfirm() {
  const [pending, setPending] = useState<PendingConfirm | null>(null);

  const confirm = useCallback(
    (options: ConfirmOptions) =>
      new Promise<boolean>((resolve) => setPending({ ...options, resolve })),
    [],
  );

  const settle = (ok: boolean) => {
    pending?.resolve(ok);
    setPending(null);
  };

  const dialog = pending ? (
    <Modal
      open
      title={pending.title}
      description={pending.description}
      confirmLabel={pending.confirmLabel ?? '삭제'}
      cancelLabel="취소"
      onConfirm={() => settle(true)}
      onClose={() => settle(false)}
    />
  ) : null;

  return { confirm, dialog };
}
