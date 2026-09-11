'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { Button, Card, FieldMessage, FormGroup, Modal, Notice, Panel, StatusPill, Tag, TextField } from '@/components/ui';

export function DesignSystemShowcase() {
  const [modalOpen, setModalOpen] = useState(false);
  return (
    <main className="board-wrap">
      <div className="container design-system">
        <header className="design-system-head">
          <p className="eyebrow">INTERNAL REFERENCE</p>
          <h1>디자인 시스템</h1>
          <p>공통 UI의 지원 상태와 테마별 표현을 한 화면에서 확인합니다.</p>
        </header>

        <section className="design-system-section" aria-labelledby="buttons-title">
          <h2 id="buttons-title">버튼과 상태</h2>
          <div className="design-system-row">
            <Button>Primary</Button><Button variant="ghost">Ghost</Button><Button variant="danger">Danger</Button>
            <Button size="sm">Small</Button><Button loading>Loading</Button>
          </div>
          <div className="design-system-row">
            <StatusPill tone="success">진행 중</StatusPill><StatusPill tone="warning">검토 대기</StatusPill><StatusPill tone="error">마감</StatusPill><Tag accent>React</Tag>
          </div>
        </section>

        <section className="design-system-section" aria-labelledby="feedback-title">
          <h2 id="feedback-title">피드백</h2>
          <div className="design-system-stack">
            <Notice>정보 안내 메시지</Notice><Notice tone="success">저장했어요.</Notice><Notice tone="warning">확인이 필요해요.</Notice><Notice tone="error">요청을 처리하지 못했어요.</Notice>
          </div>
        </section>

        <section className="design-system-section" aria-labelledby="surface-title">
          <h2 id="surface-title">카드와 폼</h2>
          <div className="design-system-grid">
            <Card hoverable><h3>Card</h3><p>기본 surface와 hover 상태입니다.</p></Card>
            <Panel><h3>Panel</h3><p>보조 정보를 담는 panel입니다.</p></Panel>
          </div>
          <div className="design-system-form">
            <FormGroup label="이메일" hint="안내 문구가 입력과 연결됩니다."><TextField type="email" placeholder="crew@example.com" /></FormGroup>
            <FormGroup label="오류 상태" error="올바른 값을 입력해 주세요."><TextField defaultValue="invalid value" /></FormGroup>
            <FieldMessage tone="success">사용할 수 있는 이름이에요.</FieldMessage>
          </div>
        </section>

        <section className="design-system-section" aria-labelledby="modal-title">
          <h2 id="modal-title">오버레이</h2>
          <Button onClick={() => setModalOpen(true)}><Icon name="i-plus" />모달 열기</Button>
        </section>
      </div>
      <Modal open={modalOpen} title="확인 다이얼로그" description="포커스 이동과 키보드 순환을 확인할 수 있어요." confirmLabel="확인" onConfirm={() => setModalOpen(false)} onClose={() => setModalOpen(false)} />
    </main>
  );
}
