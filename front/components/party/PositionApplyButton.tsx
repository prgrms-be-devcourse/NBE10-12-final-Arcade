'use client';

import { useState } from 'react';
import { applyToParty } from '@/lib/api';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { Modal } from '@/components/ui/Modal';
import { FormGroup, TextAreaField } from '@/components/ui/Field';
import { POSITION_LABELS } from '@/lib/constants';
import type { PositionType } from '@/lib/types';

export function PositionApplyButton({ partyId, position, leaderId, disabled = false }: { partyId: string; position: PositionType; leaderId: string; disabled?: boolean }) {
  const me = useCurrentUser();
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (me === undefined || me?.profile.id === undefined || me.profile.id === leaderId) return null;

  const submit = async () => {
    setSubmitting(true);
    try {
      await applyToParty(partyId, { position, message });
      setDone(true);
      setOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <button type="button" className="btn btn-primary position-apply-btn" onClick={() => setOpen(true)} disabled={disabled || done}>
        {done ? '지원 완료' : '지원하기'}
      </button>
      <Modal
        open={open}
        title={`${POSITION_LABELS[position]}로 지원하기`}
        description="성취 프로필의 모든 항목이 파티장에게 함께 전달됩니다."
        onClose={() => setOpen(false)}
        footerActions={
          <button type="button" className="btn btn-primary" onClick={submit} disabled={submitting}>
            {submitting ? '지원 중…' : '지원 완료'}
          </button>
        }
      >
        <FormGroup label="파티장에게 한마디 (선택)">
          <TextAreaField
            id={`apply-message-${partyId}-${position}`}
            maxLength={50}
            placeholder="지원 동기를 50자 이내로 남겨주세요."
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
        </FormGroup>
      </Modal>
    </>
  );
}
