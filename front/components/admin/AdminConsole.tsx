'use client';

import { useState } from 'react';
import { DataTable, type Column } from '@/components/ui/DataTable';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { StatusPill } from '@/components/ui/Tag';
import { decideHostApproval, decideReport } from '@/lib/api';
import { MOCK_ADMIN_TABS } from '@/lib/mock';
import type { AdminStat, HostApproval, ReportItem } from '@/lib/types';

const PERIODS = ['주간', '월간', '연간'] as const;

type TabKey = (typeof MOCK_ADMIN_TABS)[number]['key'];

export interface AdminPartyRow {
  id: string;
  title: string;
  leader: string;
  category: string;
  applicants: number;
  status: string;
}
export interface AdminMemberRow {
  id: string;
  name: string;
  email: string;
  position: string;
  joinedAt: string;
  status: string;
}
export interface AdminBadgeRow {
  id: string;
  label: string;
  condition: string;
  visible: boolean;
}
export interface AdminAwardRow {
  id: string;
  contest: string;
  team: string;
  rank: string;
  year: string;
  reflected: boolean;
}

interface AdminConsoleProps {
  stats: {
    kpis: AdminStat[];
    chart: { label: string; value: number; height: number }[];
    table: { period: string; general: number; host: number; leave: number; net: string }[];
  };
  parties: AdminPartyRow[];
  members: AdminMemberRow[];
  hosts: HostApproval[];
  badges: AdminBadgeRow[];
  reports: ReportItem[];
  awards: AdminAwardRow[];
}

/** 관리자 콘솔 — 좌측 탭 + 우측 패널 */
export function AdminConsole({
  stats,
  parties,
  members,
  hosts: initialHosts,
  badges,
  reports: initialReports,
  awards,
}: AdminConsoleProps) {
  const [tab, setTab] = useState<TabKey>('stats');
  const [period, setPeriod] = useState<string>('월간');
  const [search, setSearch] = useState('');
  const [hosts, setHosts] = useState(initialHosts);
  const [reports, setReports] = useState(initialReports);

  function filter<T>(rows: T[], fields: (row: T) => string[]): T[] {
    if (!search) return rows;
    return rows.filter((row) => fields(row).some((value) => value.includes(search)));
  }

  const partyColumns: Column<AdminPartyRow>[] = [
    { key: 'title', header: '제목', render: (row) => row.title },
    { key: 'leader', header: '파티장', render: (row) => row.leader },
    { key: 'category', header: '유형', render: (row) => row.category },
    { key: 'applicants', header: '지원자', render: (row) => row.applicants },
    { key: 'status', header: '상태', render: (row) => row.status },
  ];

  const memberColumns: Column<AdminMemberRow>[] = [
    { key: 'name', header: '이름', render: (row) => row.name },
    { key: 'email', header: '이메일', render: (row) => row.email },
    { key: 'position', header: '포지션', render: (row) => row.position },
    { key: 'joinedAt', header: '가입일', render: (row) => row.joinedAt },
    { key: 'status', header: '상태', render: (row) => row.status },
  ];

  const hostColumns: Column<HostApproval>[] = [
    { key: 'company', header: '회사명', render: (row) => row.company },
    { key: 'bizNumber', header: '사업자번호', render: (row) => row.bizNumber },
    { key: 'manager', header: '담당자', render: (row) => row.manager },
    { key: 'requestedAt', header: '신청일', render: (row) => row.requestedAt },
    {
      key: 'status',
      header: '처리',
      render: (row) =>
        row.status === '대기' ? (
          <>
            <button
              type="button"
              className="row-btn ok"
              onClick={async () => {
                setHosts((prev) =>
                  prev.map((item) => (item.id === row.id ? { ...item, status: '승인' } : item)),
                );
                await decideHostApproval(row.id, '승인');
              }}
            >
              승인
            </button>
            <button
              type="button"
              className="row-btn"
              onClick={async () => {
                setHosts((prev) =>
                  prev.map((item) => (item.id === row.id ? { ...item, status: '반려' } : item)),
                );
                await decideHostApproval(row.id, '반려');
              }}
            >
              반려
            </button>
          </>
        ) : (
          <StatusPill tone={row.status === '승인' ? 'live' : 'default'}>{row.status}</StatusPill>
        ),
    },
  ];

  const reportColumns: Column<ReportItem>[] = [
    { key: 'type', header: '유형', render: (row) => row.type },
    { key: 'target', header: '대상', render: (row) => row.target },
    { key: 'reporter', header: '신고자', render: (row) => row.reporter },
    { key: 'reason', header: '사유', render: (row) => row.reason },
    { key: 'createdAt', header: '접수일', render: (row) => row.createdAt },
    {
      key: 'status',
      header: '처리',
      render: (row) =>
        row.status === '대기' ? (
          <>
            <button
              type="button"
              className="row-btn ok"
              onClick={async () => {
                setReports((prev) =>
                  prev.map((item) => (item.id === row.id ? { ...item, status: '처리완료' } : item)),
                );
                await decideReport(row.id, '처리완료');
              }}
            >
              처리
            </button>
            <button
              type="button"
              className="row-btn"
              onClick={async () => {
                setReports((prev) =>
                  prev.map((item) => (item.id === row.id ? { ...item, status: '반려' } : item)),
                );
                await decideReport(row.id, '반려');
              }}
            >
              반려
            </button>
          </>
        ) : (
          <StatusPill>{row.status}</StatusPill>
        ),
    },
  ];

  const awardColumns: Column<AdminAwardRow>[] = [
    { key: 'contest', header: '공모전', render: (row) => row.contest },
    { key: 'team', header: '팀명', render: (row) => row.team },
    { key: 'rank', header: '수상', render: (row) => row.rank },
    { key: 'year', header: '연도', render: (row) => row.year },
    {
      key: 'reflected',
      header: '프로필 반영',
      render: (row) => (
        <StatusPill tone={row.reflected ? 'live' : 'default'}>
          {row.reflected ? '반영됨' : '대기'}
        </StatusPill>
      ),
    },
  ];

  return (
    <div className="admin-layout">
      <nav className="admin-nav">
        {MOCK_ADMIN_TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={tab === item.key ? 'is-active' : undefined}
            onClick={() => {
              setTab(item.key);
              setSearch('');
            }}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <div className="admin-body">
        {tab === 'stats' ? (
          <section className="admin-panel">
            <div className="admin-panel-head">
              <div>
                <h2>기간별 사용자 가입 통계</h2>
                <p>신규 가입과 주최측 전환 추이를 확인해요.</p>
              </div>
              <RadioChipGroup options={PERIODS} value={period} onChange={setPeriod} />
            </div>

            <div className="admin-kpis">
              {stats.kpis.map((kpi) => (
                <div key={kpi.label} className="admin-kpi">
                  <p className="k">{kpi.label}</p>
                  <p className="v">{kpi.value}</p>
                  <p className="d">{kpi.delta}</p>
                </div>
              ))}
            </div>

            <div className="chart-box">
              <h3 className="block-title" style={{ marginBottom: 0 }}>
                월별 신규 가입
              </h3>
              <div className="chart-bars">
                {stats.chart.map((column) => (
                  <div key={column.label} className="chart-col">
                    <span className="cv">{column.value}</span>
                    <span className="bar" style={{ height: `${column.height}%` }} />
                    <span className="cl">{column.label}</span>
                  </div>
                ))}
              </div>
              <div className="chart-legend">
                <span>
                  <i />
                  일반 회원
                </span>
                <span>
                  <i className="alt" />
                  주최측
                </span>
              </div>
            </div>

            <div style={{ marginTop: '1.375rem' }}>
              <DataTable
                columns={[
                  { key: 'period', header: '기간', render: (row) => row.period },
                  { key: 'general', header: '일반 가입', render: (row) => row.general },
                  { key: 'host', header: '주최측 가입', render: (row) => row.host },
                  { key: 'leave', header: '탈퇴', render: (row) => row.leave },
                  { key: 'net', header: '순증', render: (row) => row.net },
                ]}
                rows={stats.table}
                rowKey={(row) => row.period}
              />
            </div>
          </section>
        ) : null}

        {tab === 'party' ? (
          <AdminTablePanel
            title="파티 리스트 관리"
            description="모집글을 조회 · 수정 · 삭제해요."
            addLabel="파티 등록"
            placeholder="제목 · 파티장으로 검색"
            search={search}
            onSearch={setSearch}
            columns={partyColumns}
            rows={filter(parties, (row) => [row.title, row.leader])}
            rowKey={(row) => row.id}
          />
        ) : null}

        {tab === 'member' ? (
          <AdminTablePanel
            title="회원 관리"
            description="회원 정보를 수정하거나 상태를 변경해요."
            addLabel="회원 추가"
            placeholder="이름 · 이메일로 검색"
            search={search}
            onSearch={setSearch}
            columns={memberColumns}
            rows={filter(members, (row) => [row.name, row.email])}
            rowKey={(row) => row.id}
          />
        ) : null}

        {tab === 'host' ? (
          <AdminTablePanel
            title="사업자 가입 승인"
            description="제출된 증빙과 국세청 진위확인 결과를 보고 승인 · 반려해요."
            addLabel="수동 등록"
            placeholder="회사명 · 사업자번호로 검색"
            search={search}
            onSearch={setSearch}
            columns={hostColumns}
            rows={filter(hosts, (row) => [row.company, row.bizNumber])}
            rowKey={(row) => row.id}
          />
        ) : null}

        {tab === 'badge' ? (
          <section className="admin-panel">
            <div className="admin-panel-head">
              <div>
                <h2>뱃지 등록 · 관리</h2>
                <p>획득 조건과 노출 여부를 관리해요.</p>
              </div>
              <button type="button" className="btn btn-primary">
                뱃지 등록
              </button>
            </div>
            <div className="badge-admin-grid">
              {badges.map((badge) => (
                <div key={badge.id} className="badge-admin-card">
                  <h4>{badge.label}</h4>
                  <p>{badge.condition}</p>
                  <StatusPill tone={badge.visible ? 'live' : 'default'}>
                    {badge.visible ? '노출' : '숨김'}
                  </StatusPill>
                </div>
              ))}
            </div>
          </section>
        ) : null}

        {tab === 'report' ? (
          <AdminTablePanel
            title="신고 관리"
            description="접수된 신고를 확인하고 처리 결과를 남겨요."
            placeholder="대상 · 사유로 검색"
            search={search}
            onSearch={setSearch}
            columns={reportColumns}
            rows={filter(reports, (row) => [row.target, row.reason])}
            rowKey={(row) => row.id}
          />
        ) : null}

        {tab === 'award' ? (
          <AdminTablePanel
            title="공모전 수상 이력 관리"
            description="수상 기록을 등록하면 참여자 성취 프로필에 자동 반영돼요."
            addLabel="수상 이력 등록"
            placeholder="공모전 · 팀명으로 검색"
            search={search}
            onSearch={setSearch}
            columns={awardColumns}
            rows={filter(awards, (row) => [row.contest, row.team])}
            rowKey={(row) => row.id}
          />
        ) : null}
      </div>
    </div>
  );
}

interface AdminTablePanelProps<T> {
  title: string;
  description: string;
  addLabel?: string;
  placeholder: string;
  search: string;
  onSearch: (value: string) => void;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string;
}

function AdminTablePanel<T>({
  title,
  description,
  addLabel,
  placeholder,
  search,
  onSearch,
  columns,
  rows,
  rowKey,
}: AdminTablePanelProps<T>) {
  return (
    <section className="admin-panel">
      <div className="admin-panel-head">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        {addLabel ? (
          <button type="button" className="btn btn-primary">
            {addLabel}
          </button>
        ) : null}
      </div>
      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder={placeholder}
          value={search}
          onChange={(event) => onSearch(event.target.value)}
        />
      </div>
      <DataTable columns={columns} rows={rows} rowKey={rowKey} />
    </section>
  );
}
