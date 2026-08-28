'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { Avatar } from '@/components/ui/Avatar';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { FormGroup, SelectField } from '@/components/ui/Field';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { decideApplicant } from '@/lib/api';
import { POSITION_LABELS, POSITION_TYPES } from '@/lib/constants';
import type { Applicant, ApplicantStatus, PositionType } from '@/lib/types';

interface ApplicantManagerProps {
  applicants: Applicant[];
  parties: { id: string; title: string }[];
}

/** 마이페이지 관리 탭 — 파티/파트 필터 + 지원자 승인·거절 */
export function ApplicantManager({ applicants: initial, parties }: ApplicantManagerProps) {
  const [applicants, setApplicants] = useState(initial);
  const [partyId, setPartyId] = useState('전체');
  const [position, setPosition] = useState<PositionType | '전체'>('전체');

  const visible = useMemo(
    () =>
      applicants.filter(
        (applicant) =>
          applicant.status === 'pending' &&
          (partyId === '전체' || applicant.partyId === partyId) &&
          (position === '전체' || applicant.position === position),
      ),
    [applicants, partyId, position],
  );

  /** 파티 + 포지션 단위로 묶어 보여준다 */
  const groups = useMemo(() => {
    const map = new Map<string, Applicant[]>();
    visible.forEach((applicant) => {
      const key = `${applicant.partyName} · ${POSITION_LABELS[applicant.position]}`;
      map.set(key, [...(map.get(key) ?? []), applicant]);
    });
    return Array.from(map.entries());
  }, [visible]);

  const decide = async (id: string, status: ApplicantStatus) => {
    setApplicants((prev) =>
      prev.map((applicant) => (applicant.id === id ? { ...applicant, status } : applicant)),
    );
    await decideApplicant(id, status);
  };

  return (
    <>
      <div className="mgmt-filter-row">
        <FormGroup label="파티" htmlFor="mgmtPartySelect">
          <SelectField
            id="mgmtPartySelect"
            value={partyId}
            onChange={(event) => setPartyId(event.target.value)}
          >
            <option value="전체">전체 파티</option>
            {parties.map((party) => (
              <option key={party.id} value={party.id}>
                {party.title}
              </option>
            ))}
          </SelectField>
        </FormGroup>
        <FormGroup label="파트" htmlFor="mgmtPositionSelect">
          <SelectField
            id="mgmtPositionSelect"
            value={position}
            onChange={(event) => setPosition(event.target.value as PositionType | '전체')}
          >
            <option value="전체">전체 파트</option>
            {POSITION_TYPES.map((type) => (
              <option key={type} value={type}>
                {POSITION_LABELS[type]}
              </option>
            ))}
          </SelectField>
        </FormGroup>
      </div>

      <p className="mgmt-result-line">
        {visible.length > 0 ? (
          <>
            선택한 조건의 지원자 <b>{visible.length}명</b>을 보고 있어요.
          </>
        ) : (
          '선택한 조건에 해당하는 지원자가 없어요.'
        )}
      </p>

      <div style={{ marginTop: '1.5rem' }}>
        {groups.map(([label, groupApplicants]) => (
          <div key={label} className="position-group">
            <div className="position-group-head">
              <h4>{label}</h4>
              <span className="frac">지원자 {groupApplicants.length}명</span>
            </div>
            <div className="applicant-card-grid">
              {groupApplicants.map((applicant) => (
                <article key={applicant.id} className="applicant-card">
                  <Avatar initial={applicant.user.initial} avatarUrl={applicant.user.avatarUrl} size="small" />
                  <div className="applicant-card-body">
                    <div className="applicant-card-top">
                      <h4>{applicant.user.name}</h4>
                    </div>
                    <p className="applicant-role">선호 포지션 · {applicant.user.role}</p>
                    <p className="applicant-activity">{applicant.achievements.join(' · ')}</p>
                    <p className="applicant-word">
                      <span className="wlabel">파티장에게 한마디</span>“{applicant.message}”
                    </p>
                    <ChipRow>
                      {applicant.skills.map((skill) => (
                        <SkillChip key={skill}>{skill}</SkillChip>
                      ))}
                    </ChipRow>
                    <div className="applicant-card-actions">
                      <button
                        type="button"
                        className="btn btn-ghost"
                        style={{ padding: '0.5rem 0.875rem' }}
                        onClick={() => decide(applicant.id, 'rejected')}
                      >
                        거절
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary"
                        style={{ padding: '0.5rem 0.875rem' }}
                        onClick={() => decide(applicant.id, 'accepted')}
                      >
                        승인
                      </button>
                      <SendMessageButton recipient={applicant.user} variant="icon" />
                      <Link className="card-link" href={`/profile/${applicant.user.id}`}>
                        자세히 보기 →
                      </Link>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </div>
        ))}
      </div>
      {groups.length === 0 ? <p className="notif-empty">조건에 맞는 지원자가 없어요.</p> : null}
    </>
  );
}
