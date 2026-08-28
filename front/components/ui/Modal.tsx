'use client';

import { useEffect, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Icon } from '@/components/icons/Icon';

interface ModalProps {
  open: boolean;
  title: string;
  description?: string;
  children?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void;
  onClose: () => void;
}

/**
 * 목업의 #appModal — 확인/취소를 갖는 공용 다이얼로그.
 *
 * 화면 전체를 덮어야 하므로 어디서 호출하든 body 로 포털해서 그린다.
 * 그냥 제자리에 그리면 조상 중 하나라도 transform · filter · contain 을 갖는 순간
 * position:fixed 의 기준이 화면이 아니라 그 조상이 되어, 모달이 헤더 같은 좁은 상자 안에 갇힌다.
 * (실제로 헤더에 스크롤 숨김 transform 을 넣자 로그아웃 확인창이 헤더 안에서 열렸다)
 */
export function Modal({
  open,
  title,
  description,
  children,
  confirmLabel,
  cancelLabel = '닫기',
  onConfirm,
  onClose,
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  // 서버에는 document 가 없다. 모달은 항상 닫힌 채로 시작해 사용자가 열므로 이 분기로 충분하다.
  if (!open || typeof document === 'undefined') return null;

  return createPortal(
    <div
      className="modal-overlay"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-head">
          <div>
            <h3>{title}</h3>
            {description ? <p>{description}</p> : null}
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="닫기">
            <Icon name="i-x" />
          </button>
        </div>
        {/* 확인창처럼 본문이 없는 경우엔 빈 띠가 남지 않도록 아예 그리지 않는다 */}
        {children ? <div className="modal-body">{children}</div> : null}
        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            {cancelLabel}
          </button>
          {confirmLabel ? (
            <button type="button" className="btn btn-primary" onClick={onConfirm}>
              {confirmLabel}
            </button>
          ) : null}
        </div>
      </div>
    </div>,
    document.body,
  );
}
