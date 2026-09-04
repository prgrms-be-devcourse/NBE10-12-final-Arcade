'use client';

import { useState } from 'react';
import { applyToParty } from '@/lib/api';
import { useCurrentUser } from '@/lib/hooks/useCurrentUser';
import { DDay, Tag } from '@/components/ui/Tag';
import { TextAreaField } from '@/components/ui/Field';
import { POSITION_LABELS } from '@/lib/constants';
import type { PartyDetail, PositionType } from '@/lib/types';

const MESSAGE_MAX = 50;

/** 파티 상세 사이드바의 지원 패널 */
export function ApplyPanel({ party }: { party: PartyDetail }) {
  const me = useCurrentUser();
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<PositionType | null>(null);
  const [message, setMessage] = useState('');
  const [done, setDone] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const openPositions = party.positions.filter(
    (position) => position.capacity > 0 && position.filledCount < position.capacity,
  );

  // 로그인 사용자 정보를 확인하기 전에는 지원 UI를 그리지 않는다. 작성자 본인의 파티에서
  // 지원 탭이 잠깐 보였다가 사라지는 현상을 방지한다.
  const isPartyOwner = me?.profile.id === party.leader.id;
  if (me === undefined || isPartyOwner || party.status !== 'RECRUITING') return null;

  const submit = async () => {
    if (!position) return;
    setSubmitting(true);
    try {
      await applyToParty(party.id, { position, message });
      setDone(
        `${POSITION_LABELS[position]} 포지션으로 지원이 완료됐어요. 파티장이 확인하면 알림으로 알려드릴게요.`,
      );
      setOpen(false);
      setPosition(null);
      setMessage('');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="side-card apply-panel">
      <h4>지원하기</h4>
      <p className="apply-note">
        {openPositions.length > 0 ? '지원할 포지션을 선택하세요' : '지원 가능한 포지션'}
      </p>
      <div className="apply-positions">
        {openPositions.length > 0 ? (
          openPositions.map((slot, index) => (
            <button
              key={`${slot.type}-${index}`}
              type="button"
              className={
                position === slot.type ? 'apply-position-chip is-active' : 'apply-position-chip'
              }
              aria-pressed={position === slot.type}
              onClick={() => setPosition(slot.type)}
            >
              {POSITION_LABELS[slot.type]} {slot.filledCount}/{slot.capacity}
            </button>
          ))
        ) : (
          <Tag>모집 마감</Tag>
        )}
      </div>

      {!done && openPositions.length > 0 ? (
        <button
          type="button"
          className="btn btn-primary"
          style={{ width: '100%', marginTop: '0.875rem' }}
          onClick={() => setOpen(true)}
          hidden={open}
          disabled={!position}
        >
          {position ? `${POSITION_LABELS[position]}로 지원하기` : '포지션을 선택해 주세요'}
        </button>
      ) : null}

      {open ? (
        <div className="apply-form">
          <p className="apply-selected">
            지원 포지션 <b>{position ? POSITION_LABELS[position] : '-'}</b>
          </p>
          <label className="form-label" htmlFor="applyMessage">
            파티장에게 한마디 (선택)
          </label>
          <TextAreaField
            id="applyMessage"
            maxLength={MESSAGE_MAX}
            placeholder="지원 동기를 50자 이내로 남겨주세요."
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
          <div className="apply-form-foot">
            <span className="char-count">
              {message.length} / {MESSAGE_MAX}
            </span>
            <div className="apply-form-btns">
              <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>
                취소
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={submit}
                disabled={submitting || !position}
              >
                {submitting ? '지원 중…' : '지원 완료'}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {done ? <p className="apply-done-note">{done}</p> : null}

      <p className="apply-note">
        지원 시 성취 프로필의 모든 항목이 그대로 첨부되고, 남긴 한마디는 파티장 지원자 목록에 함께
        표시됩니다.
      </p>
      <div className="apply-dday">
        <span className="label">마감까지</span>
        <DDay>{party.dday}</DDay>
      </div>
    </div>
  );
}
