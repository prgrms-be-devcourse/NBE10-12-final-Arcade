'use client';

import { useState } from 'react';
import { Checklist } from '@/components/team/Checklist';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { TextAreaField } from '@/components/ui/Field';
import { Tag, TagRow } from '@/components/ui/Tag';
import { finishTodo, saveSoloMemo } from '@/lib/api';
import type { ChecklistItem } from '@/lib/types';

interface SoloSpaceProps {
  space: {
    id: string;
    title: string;
    type: string;
    createdAt: string;
    memo: string;
    checklist: ChecklistItem[];
  };
  ownerName: string;
}

/** 개인 TODO 상세 = 승인 절차가 없는 1인 팀 스페이스 */
export function SoloSpace({ space, ownerName }: SoloSpaceProps) {
  const [memo, setMemo] = useState(space.memo);
  const [memoSaved, setMemoSaved] = useState(false);
  const [finished, setFinished] = useState(false);

  const total = space.checklist.length;
  const done = space.checklist.filter((item) => item.state === 'done').length;

  return (
    <DetailGrid
      main={
        <>
          <div className="detail-header">
            <TagRow>
              <Tag>개인</Tag>
              <Tag>{space.type}</Tag>
            </TagRow>
          </div>

          <h1 className="detail-title">{space.title}</h1>
          <p className="detail-meta">개인 목록 · {space.createdAt} 생성 · 혼자 진행</p>

          <Block
            title="개인 메모"
            description="공모전 연동 대신 나만 보는 메모를 남길 수 있어요. 완료해도 전시관에는 공개되지 않습니다."
            className="solo-memo"
          >
            <TextAreaField
              value={memo}
              onChange={(event) => {
                setMemo(event.target.value);
                setMemoSaved(false);
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.625rem' }}>
              <button
                type="button"
                className="btn btn-ghost"
                style={{ padding: '0.5625rem 1.125rem', fontSize: '.82rem' }}
                onClick={async () => {
                  await saveSoloMemo(space.id, memo);
                  setMemoSaved(true);
                }}
              >
                메모 저장
              </button>
            </div>
            {memoSaved ? <p className="tool-done-note">메모를 저장했어요.</p> : null}
          </Block>

          <Block title="진행 체크리스트">
            <Checklist todoId={space.id} items={space.checklist} ownerName={ownerName} />
          </Block>
        </>
      }
      side={
        <>
          <SideCard title="목록 정보">
            <div className="stat-row">
              <span className="label">유형</span>
              <span className="value">{space.type}</span>
            </div>
            <div className="stat-row">
              <span className="label">항목 수</span>
              <span className="value">{total}</span>
            </div>
            <div className="stat-row">
              <span className="label">완료</span>
              <span className="value">{done}</span>
            </div>
            <div className="stat-row">
              <span className="label">진행률</span>
              <span className="value">{total ? Math.round((done / total) * 100) : 0}%</span>
            </div>
          </SideCard>

          <SideCard title="목록 관리" className="leader-tools">
            <p className="leader-tools-note">
              완료하면 마이페이지 개인 TODO 목록에서 완료 상태로 표시돼요.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              disabled={finished}
              onClick={async () => {
                await finishTodo(space.id);
                setFinished(true);
              }}
            >
              목록 완료 처리
            </button>
            {finished ? (
              <p className="tool-done-note">
                완료 처리했어요. 개인 TODO 목록에서 확인할 수 있습니다.
              </p>
            ) : null}
          </SideCard>
        </>
      }
    />
  );
}
