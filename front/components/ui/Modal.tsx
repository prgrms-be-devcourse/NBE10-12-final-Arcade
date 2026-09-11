'use client';

import { useEffect, useId, useRef, type ReactNode } from 'react';
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
  /**
   * 확인 버튼 옆에 붙는 짧은 안내·오류 문구.
   *
   * 본문(.modal-body)은 스크롤되므로 그 안에 오류를 두면 위쪽을 보고 있을 때 눈에 띄지 않는다 -
   * 버튼을 눌렀는데 아무 일도 안 일어난 것처럼 보인다. 버튼과 같은 줄에 두어 항상 보이게 한다.
   */
  footNote?: ReactNode;
  /**
   * 닫을 수 있는지. 기본은 닫을 수 있다.
   * false 면 닫기 버튼·ESC·배경 클릭·취소 버튼이 모두 사라진다 -
   * 약관 동의처럼 통과해야만 다음으로 갈 수 있는 화면에 쓴다.
   */
  dismissible?: boolean;
  /** 기본 헤더 닫기(X)를 숨기고, 푸터의 닫기 버튼만 사용한다. */
  headerClose?: boolean;
  /** 취소·확인 버튼 앞에 놓을 추가 푸터 액션. */
  footerActions?: ReactNode;
  confirmVariant?: 'primary' | 'danger';
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
  dismissible = true,
  headerClose = true,
  footerActions,
  footNote,
  confirmVariant = 'primary',
}: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLElement | null>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;
    triggerRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const frame = requestAnimationFrame(() => {
      const dialog = dialogRef.current;
      if (!dialog) return;
      const initial = dialog.querySelector<HTMLElement>('[autofocus], [data-modal-initial-focus], button, input, select, textarea, [tabindex]:not([tabindex="-1"])');
      (initial ?? dialog).focus();
    });
    return () => {
      cancelAnimationFrame(frame);
      triggerRef.current?.focus();
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && dismissible) onClose();
      if (event.key !== 'Tab') return;
      const dialog = dialogRef.current;
      if (!dialog) return;
      const focusable = Array.from(dialog.querySelectorAll<HTMLElement>('a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'));
      if (focusable.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onClose, dismissible]);

  // 서버에는 document 가 없다. 모달은 항상 닫힌 채로 시작해 사용자가 열므로 이 분기로 충분하다.
  if (!open || typeof document === 'undefined') return null;

  return createPortal(
    <div
      className="modal-overlay"
      role="presentation"
      onClick={(event) => {
        if (dismissible && event.target === event.currentTarget) onClose();
      }}
    >
      <div ref={dialogRef} className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}>
        <div className="modal-head">
          <div>
            <h3 id={titleId}>{title}</h3>
            {description ? <p>{description}</p> : null}
          </div>
          {dismissible && headerClose ? (
            <button type="button" className="modal-close" onClick={onClose} aria-label="닫기">
              <Icon name="i-x" />
            </button>
          ) : null}
        </div>
        {/* 확인창처럼 본문이 없는 경우엔 빈 띠가 남지 않도록 아예 그리지 않는다 */}
        {children ? <div className="modal-body">{children}</div> : null}
        <div className="modal-foot">
          {footNote ? <div className="modal-foot-note">{footNote}</div> : null}
          {footerActions ? <div className="modal-foot-actions">{footerActions}</div> : null}
          {dismissible ? (
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              {cancelLabel}
            </button>
          ) : null}
          {confirmLabel ? (
            <button type="button" className={confirmVariant === 'danger' ? 'btn is-danger' : 'btn btn-primary'} onClick={onConfirm}>
              {confirmLabel}
            </button>
          ) : null}
        </div>
      </div>
    </div>,
    document.body,
  );
}
