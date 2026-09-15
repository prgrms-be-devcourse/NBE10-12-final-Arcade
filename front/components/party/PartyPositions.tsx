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
  const blocked = party.myApplicationStatus != null || appliedPosition != null;

  return (
    <PositionScroller className={party.status !== 'RECRUITING' ? 'is-closed' : undefined}>
      {party.positions.map((position, index) => {
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
                <StatusPill tone={party.myApplicationStatus === 'REJECTED' ? 'error' : party.myApplicationStatus === 'APPROVED' ? 'success' : 'pending'}>
                  {party.myApplicationStatus === 'REJECTED' ? '지원 이력' : party.myApplicationStatus === 'APPROVED' ? '참여 확정' : '지원 중'}
                </StatusPill>
              ) : null}
            </div>
            <PositionApplyButton
              partyId={party.id}
              position={position.type}
              leaderId={party.leader.id}
              disabled={!isAvailable || blocked}
              onApplied={() => setAppliedPosition(position.type)}
            />
          </div>
        );
      })}
    </PositionScroller>
  );
}
