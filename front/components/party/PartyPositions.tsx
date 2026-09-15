'use client';

import { useState } from 'react';
import { PositionApplyButton } from './PositionApplyButton';
import { PositionScroller } from './PositionScroller';
import { StatusPill } from '@/components/ui/Tag';
import { POSITION_LABELS } from '@/lib/constants';
import type { Party, PositionType } from '@/lib/types';

export function PartyPositions({ party }: { party: Party }) {
  const [appliedPosition, setAppliedPosition] = useState<PositionType | null>(
    party.myApplicationPosition ?? null,
  );
  const [appliedStatus, setAppliedStatus] = useState(party.myApplicationStatus ?? null);
  const blocked = appliedStatus != null || appliedPosition != null;
  const applicationStatus = appliedStatus;

  return (
    <PositionScroller className={party.status !== 'RECRUITING' ? 'is-closed' : undefined}>
      {[...party.positions]
        .sort((a, b) => {
          const complete = (position: typeof a) => position.capacity === 0 || position.filledCount >= position.capacity;
          return Number(complete(a)) - Number(complete(b));
        })
        .map((position, index) => {
        const complete = position.capacity === 0 || position.filledCount >= position.capacity;
        const isAvailable = party.status === 'RECRUITING' && position.capacity > 0 && !complete;
        const isApplied = appliedPosition === position.type;
        const state = position.capacity === 0 ? 'none' : complete ? 'full' : '';
        return (
          <div key={`${position.type}-${index}`} className={['position-row', state, isAvailable ? 'is-available' : null].filter(Boolean).join(' ')}>
            <div className="position-summary">
              <p className="name">{POSITION_LABELS[position.type]}</p>
              <span className="frac">{position.filledCount}/{position.capacity}</span>
              {isApplied ? (
                <StatusPill tone={applicationStatus === 'REJECTED' ? 'error' : applicationStatus === 'APPROVED' ? 'success' : 'pending'}>
                  {applicationStatus === 'REJECTED' ? '지원 이력' : applicationStatus === 'APPROVED' ? '참여 확정' : '지원 중'}
                </StatusPill>
              ) : null}
            </div>
            <PositionApplyButton
              partyId={party.id}
              position={position.type}
              leaderId={party.leader.id}
              disabled={!isAvailable || blocked}
              onApplied={() => {
                setAppliedPosition(position.type);
                setAppliedStatus('PENDING');
              }}
            />
          </div>
        );
        })}
    </PositionScroller>
  );
}
