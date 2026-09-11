'use client';

import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { DataTable, type Column } from '@/components/ui/DataTable';
import { Modal } from '@/components/ui/Modal';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { Notice } from '@/components/ui/Notice';
import { Pagination } from '@/components/ui/Pagination';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { StatusPill } from '@/components/ui/Tag';
import { FormGroup, TextAreaField } from '@/components/ui/Field';
import {
  ApiError,
  decideHostApproval,
  decideReport,
  deleteAdminParty,
  downloadEvidence,
  fetchAdminEvidences,
  fetchAdminMembers,
  fetchAdminParties,
  fetchGoalDetail,
  fetchMemberHistory,
  reviewEvidence,
  updateMemberStatus,
  updatePartyHidden,
  type AdminEvidenceRow,
  type AdminMemberHistory,
  type AdminMemberRow,
  type AdminPage,
  type AdminPartyRow,
  type EvidenceStatus,
  type GoalDetailResponse,
} from '@/lib/api';
import { httpUrlOrNull } from '@/lib/externalUrl';
import {
  GOAL_STATUS_LABELS,
  GOAL_TYPE_LABELS,
  PARTY_STATUS_LABELS,
  TOPIC_TYPE_LABELS,
} from '@/lib/constants';
import type {
  AdminStat,
  GoalStatus,
  GoalType,
  HostApproval,
  PartyStatus,
  ReportItem,
  TopicType,
} from '@/lib/types';

const PERIODS = ['주간', '월간', '연간'] as const;

/**
 * 좌측 탭.
 *
 * `hidden: true` 는 백엔드가 아직 없는 화면이다. 패널 코드는 그대로 두고 내비게이션에서만 감춘다 -
 * 서버가 생기면 이 플래그만 지우면 된다.
 */
const ADMIN_TABS = [
  { key: 'party', label: '파티 리스트 관리' },
  { key: 'member', label: '회원 관리' },
  { key: 'evidence', label: '성취 증빙 검수' },
  { key: 'stats', label: '가입 통계', hidden: true },
  { key: 'host', label: '사업자 가입 승인', hidden: true },
  { key: 'badge', label: '뱃지 등록 · 관리', hidden: true },
  { key: 'report', label: '신고 관리', hidden: true },
  { key: 'award', label: '공모전 수상 이력', hidden: true },
] as const satisfies readonly { key: string; label: string; hidden?: boolean }[];

type TabKey = (typeof ADMIN_TABS)[number]['key'];

const VISIBLE_TABS = ADMIN_TABS.filter((tab) => !('hidden' in tab && tab.hidden));

const EVIDENCE_STATUS_LABELS: Record<EvidenceStatus, string> = {
  PENDING: '검수 대기',
  APPROVED: '승인',
  REJECTED: '반려',
};

const EVIDENCE_STATUSES = Object.keys(EVIDENCE_STATUS_LABELS) as EvidenceStatus[];

/** 파티 숨김 필터. 서버는 hidden 을 생략하면 전부 돌려준다 */
const HIDDEN_FILTERS = ['전체', '공개', '숨김'] as const;
type HiddenFilter = (typeof HIDDEN_FILTERS)[number];

function hiddenParam(filter: HiddenFilter): boolean | undefined {
  if (filter === '공개') return false;
  if (filter === '숨김') return true;
  return undefined;
}

/** 관리자만 보는 값이라 원문 enum 을 그대로 두지 않고 서비스 화면과 같은 말로 맞춘다 */
const ROLE_LABELS: Record<AdminMemberRow['role'], string> = {
  MEMBER: '일반',
  HOST: '주최측',
  ADMIN: '관리자',
};

/** 바이트를 사람이 읽는 단위로. 관리자 표에는 KB·MB 면 충분하다 */
function fileSize(bytes: number | null): string {
  if (bytes == null) return '-';
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}

/** ISO 문자열에서 날짜만. 표 한 칸에 시각까지 넣으면 줄바꿈이 생긴다 */
function dateOnly(value: string | null): string {
  return value ? value.slice(0, 10) : '-';
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
  hosts: HostApproval[];
  badges: AdminBadgeRow[];
  reports: ReportItem[];
  awards: AdminAwardRow[];
}

/** 관리자 콘솔 — 좌측 탭 + 우측 패널 */
export function AdminConsole({
  stats,
  hosts: initialHosts,
  badges,
  reports: initialReports,
  awards,
}: AdminConsoleProps) {
  const [tab, setTab] = useState<TabKey>(VISIBLE_TABS[0].key);
  const [period, setPeriod] = useState<string>('월간');
  const [search, setSearch] = useState('');
  const [hosts, setHosts] = useState(initialHosts);
  const [reports, setReports] = useState(initialReports);

  // 파티·회원·증빙은 서버가 페이지 단위로 내려준다. 탭·검색·페이지가 바뀌면 다시 불러온다.
  const [parties, setParties] = useState<AdminPage<AdminPartyRow> | null>(null);
  const [members, setMembers] = useState<AdminPage<AdminMemberRow> | null>(null);
  const [evidences, setEvidences] = useState<AdminPage<AdminEvidenceRow> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  // 내려받기·승인처럼 목록과 무관한 실패. 표를 비우지 않고 표 위에 띄운다
  const [actionError, setActionError] = useState('');

  // 증빙 검수 상태 필터
  const [evidenceStatus, setEvidenceStatus] = useState<EvidenceStatus>('PENDING');

  // 파티 숨김 필터
  const [hiddenFilter, setHiddenFilter] = useState<HiddenFilter>('전체');

  // 정지 사유 입력. 해제는 사유가 없어 바로 처리한다
  const [suspending, setSuspending] = useState<AdminMemberRow | null>(null);
  const [suspendReason, setSuspendReason] = useState('');
  const [suspendError, setSuspendError] = useState('');

  // 증빙 상세 보기 - 표에 없는 항목(팀 참가 여부 · 성취 상태 · 대회 링크 원문)까지 확인한다
  const [detailOf, setDetailOf] = useState<AdminEvidenceRow | null>(null);
  const [goalDetail, setGoalDetail] = useState<GoalDetailResponse | null>(null);
  const [detailError, setDetailError] = useState('');

  // 회원 이력 보기
  const [historyOf, setHistoryOf] = useState<AdminMemberRow | null>(null);
  const [history, setHistory] = useState<AdminMemberHistory | null>(null);
  const [historyError, setHistoryError] = useState('');

  const { confirm, dialog } = useConfirm();

  // 반려 사유 입력. 사유 없이는 서버가 거절하므로(400-1) 버튼만으로 끝낼 수 없다.
  const [rejecting, setRejecting] = useState<AdminEvidenceRow | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [rejectError, setRejectError] = useState('');
  const [working, setWorking] = useState(false);

  const message = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    setActionError('');
    try {
      if (tab === 'party') setParties(await fetchAdminParties(search, page, hiddenParam(hiddenFilter)));
      else if (tab === 'member') setMembers(await fetchAdminMembers(search, page));
      else if (tab === 'evidence') setEvidences(await fetchAdminEvidences(evidenceStatus, page));
    } catch (error) {
      setLoadError(message(error, '목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'));
    } finally {
      setLoading(false);
    }
  }, [tab, search, page, evidenceStatus, hiddenFilter]);

  useEffect(() => {
    if (tab !== 'party' && tab !== 'member' && tab !== 'evidence') return;
    // 검색어를 한 글자씩 칠 때마다 요청이 나가지 않게 잠깐 모아서 보낸다
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load, tab]);

  /** 목록을 건드리는 관리 동작 공통 처리 - 실패하면 표 위에 알리고 목록은 그대로 둔다 */
  const runAction = async (action: () => Promise<void>, fallback: string) => {
    setWorking(true);
    setActionError('');
    try {
      await action();
      await load();
    } catch (error) {
      setActionError(message(error, fallback));
    } finally {
      setWorking(false);
    }
  };

  const togglePartyHidden = (row: AdminPartyRow) =>
    runAction(
      () => updatePartyHidden(row.id, !row.hidden),
      row.hidden ? '숨김을 해제하지 못했어요.' : '숨김 처리하지 못했어요.',
    );

  const removeParty = async (row: AdminPartyRow) => {
    const ok = await confirm({
      title: '이 파티를 삭제할까요?',
      description: `삭제하면 '${row.partyName}' 의 지원 기록 · 좋아요 · 북마크도 함께 사라져요. 되돌릴 수 없어요.`,
      confirmLabel: '삭제',
    });
    if (!ok) return;
    await runAction(() => deleteAdminParty(row.id), '파티를 삭제하지 못했어요.');
  };

  /** 정지는 사유를 받고, 해제는 확인만 거친다 */
  const toggleMemberActive = async (row: AdminMemberRow) => {
    if (!row.active) {
      const ok = await confirm({
        title: '정지를 해제할까요?',
        description: `${row.name} 님이 다시 서비스를 이용할 수 있게 돼요.`,
        confirmLabel: '정지 해제',
      });
      if (!ok) return;
      await runAction(() => updateMemberStatus(row.id, true), '정지를 해제하지 못했어요.');
      return;
    }
    setSuspending(row);
    setSuspendReason('');
    setSuspendError('');
  };

  const submitSuspend = async () => {
    if (!suspending || working) return;
    if (!suspendReason.trim()) {
      setSuspendError('정지 사유를 입력해 주세요.');
      return;
    }

    setWorking(true);
    setSuspendError('');
    try {
      await updateMemberStatus(suspending.id, false, suspendReason.trim());
      setSuspending(null);
      setSuspendReason('');
      await load();
    } catch (error) {
      setSuspendError(message(error, '정지하지 못했어요. 잠시 후 다시 시도해 주세요.'));
    } finally {
      setWorking(false);
    }
  };

  const openEvidenceDetail = async (row: AdminEvidenceRow) => {
    setDetailOf(row);
    setGoalDetail(null);
    setDetailError('');
    try {
      setGoalDetail(await fetchGoalDetail(row.goalId));
    } catch (error) {
      setDetailError(message(error, '성취 정보를 불러오지 못했어요.'));
    }
  };

  const openHistory = async (row: AdminMemberRow) => {
    setHistoryOf(row);
    setHistory(null);
    setHistoryError('');
    try {
      setHistory(await fetchMemberHistory(row.id));
    } catch (error) {
      setHistoryError(message(error, '이력을 불러오지 못했어요.'));
    }
  };

  /** 승인·반려 후 목록을 다시 읽는다. 상태 필터가 걸려 있으면 처리한 행은 목록에서 빠진다 */
  const submitReview = async (row: AdminEvidenceRow, status: 'APPROVED' | 'REJECTED', note?: string) => {
    setWorking(true);
    setRejectError('');
    try {
      await reviewEvidence(row.goalId, status, note);
      setRejecting(null);
      setRejectNote('');
      await load();
    } catch (error) {
      const text = message(error, '처리하지 못했어요. 잠시 후 다시 시도해 주세요.');
      if (status === 'REJECTED') setRejectError(text);
      else setActionError(text);
    } finally {
      setWorking(false);
    }
  };

  function filter<T>(rows: T[], fields: (row: T) => string[]): T[] {
    if (!search) return rows;
    return rows.filter((row) => fields(row).some((value) => value.includes(search)));
  }

  const partyColumns: Column<AdminPartyRow>[] = [
    { key: 'title', header: '제목', render: (row) => row.title },
    { key: 'ownerName', header: '파티장', render: (row) => row.ownerName },
    {
      key: 'topicType',
      header: '유형',
      render: (row) => TOPIC_TYPE_LABELS[row.topicType as TopicType] ?? row.topicType,
    },
    { key: 'applicantCount', header: '지원자', render: (row) => row.applicantCount ?? 0 },
    { key: 'deadline', header: '마감', render: (row) => dateOnly(row.deadline) },
    {
      key: 'status',
      header: '상태',
      render: (row) =>
        row.hidden ? (
          <StatusPill>숨김</StatusPill>
        ) : (
          <StatusPill tone="live">
            {PARTY_STATUS_LABELS[row.status as PartyStatus] ?? row.status}
          </StatusPill>
        ),
    },
    {
      key: 'actions',
      header: '처리',
      render: (row) => (
        <div className="admin-actions">
          <button
            type="button"
            className="row-btn"
            disabled={working}
            onClick={() => togglePartyHidden(row)}
          >
            {row.hidden ? '숨김 해제' : '숨김'}
          </button>
          <button
            type="button"
            className="row-btn danger"
            disabled={working}
            onClick={() => removeParty(row)}
          >
            삭제
          </button>
        </div>
      ),
    },
  ];

  const memberColumns: Column<AdminMemberRow>[] = [
    { key: 'name', header: '이름', render: (row) => row.name },
    { key: 'email', header: '이메일', render: (row) => row.email },
    { key: 'nickname', header: '닉네임', render: (row) => row.nickname ?? '-' },
    { key: 'role', header: '권한', render: (row) => ROLE_LABELS[row.role] ?? row.role },
    { key: 'createDate', header: '가입일', render: (row) => dateOnly(row.createDate) },
    {
      key: 'active',
      header: '상태',
      render: (row) => (
        <StatusPill tone={row.active ? 'live' : 'default'}>{row.active ? '활성' : '정지'}</StatusPill>
      ),
    },
    {
      key: 'actions',
      header: '처리',
      render: (row) => (
        <div className="admin-actions">
          <button
            type="button"
            className="row-btn"
            disabled={working}
            onClick={() => openHistory(row)}
          >
            이력
          </button>
          {/* 관리자 계정은 서버가 정지를 거부한다(409-2). 버튼부터 내려 헛클릭을 막는다 */}
          {row.role !== 'ADMIN' ? (
            <button
              type="button"
              className={row.active ? 'row-btn danger' : 'row-btn ok'}
              disabled={working}
              onClick={() => toggleMemberActive(row)}
            >
              {row.active ? '정지' : '정지 해제'}
            </button>
          ) : null}
        </div>
      ),
    },
  ];

  const evidenceColumns: Column<AdminEvidenceRow>[] = [
    { key: 'ownerName', header: '올린 사람', render: (row) => row.ownerName },
    {
      key: 'title',
      header: '대회명',
      render: (row) => (
        <button
          type="button"
          className="admin-link"
          title="성취 내용 보기"
          onClick={() => openEvidenceDetail(row)}
        >
          {row.title}
        </button>
      ),
    },
    { key: 'result', header: '수상', render: (row) => row.result ?? '-' },
    { key: 'awardDate', header: '수상일', render: (row) => dateOnly(row.awardDate) },
    {
      key: 'fileName',
      header: '증빙 파일',
      render: (row) => (
        <span className="admin-file">
          {row.fileName ?? '-'}
          <i>{fileSize(row.size)}</i>
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      render: (row) => (
        <StatusPill tone={row.status === 'APPROVED' ? 'live' : 'default'}>
          {EVIDENCE_STATUS_LABELS[row.status]}
        </StatusPill>
      ),
    },
    {
      key: 'reviewNote',
      header: '반려 사유',
      render: (row) => row.reviewNote ?? '-',
    },
    {
      key: 'actions',
      header: '처리',
      render: (row) => (
        <div className="admin-actions">
          <button
            type="button"
            className="row-btn"
            disabled={working}
            onClick={async () => {
              try {
                await downloadEvidence(row.goalId, row.fileName);
              } catch (error) {
                setActionError(message(error, '증빙 파일을 내려받지 못했어요.'));
              }
            }}
          >
            내려받기
          </button>
          {row.status !== 'APPROVED' ? (
            <button
              type="button"
              className="row-btn ok"
              disabled={working}
              onClick={() => submitReview(row, 'APPROVED')}
            >
              승인
            </button>
          ) : null}
          {row.status !== 'REJECTED' ? (
            <button
              type="button"
              className="row-btn danger"
              disabled={working}
              onClick={() => {
                setRejecting(row);
                setRejectNote('');
                setRejectError('');
              }}
            >
              반려
            </button>
          ) : null}
        </div>
      ),
    },
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
        {VISIBLE_TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={tab === item.key ? 'is-active' : undefined}
            aria-pressed={tab === item.key}
            onClick={() => {
              setTab(item.key);
              setSearch('');
              setPage(0);
              setLoadError('');
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
            description="모집글을 조회하고 숨기거나 삭제해요. 삭제는 되돌릴 수 없어요."
            placeholder="제목 · 파티명으로 검색"
            search={search}
            onSearch={(value) => {
              setSearch(value);
              setPage(0);
            }}
            columns={partyColumns}
            rows={parties?.items ?? []}
            rowKey={(row) => String(row.id)}
            loading={loading}
            error={loadError}
            emptyMessage="조건에 맞는 파티가 없어요."
            page={parties?.page ?? 0}
            totalPages={parties?.totalPages ?? 1}
            onPage={setPage}
            notice={actionError ? <Notice tone="error">{actionError}</Notice> : null}
            toolbar={
              <RadioChipGroup
                options={HIDDEN_FILTERS}
                value={hiddenFilter}
                onChange={(value) => {
                  setHiddenFilter(value as HiddenFilter);
                  setPage(0);
                }}
              />
            }
          />
        ) : null}

        {tab === 'member' ? (
          <AdminTablePanel
            title="회원 관리"
            description="회원 이력을 확인하고 정지하거나 해제해요."
            placeholder="이름 · 이메일 · 닉네임으로 검색"
            search={search}
            onSearch={(value) => {
              setSearch(value);
              setPage(0);
            }}
            columns={memberColumns}
            rows={members?.items ?? []}
            rowKey={(row) => String(row.id)}
            loading={loading}
            error={loadError}
            emptyMessage="조건에 맞는 회원이 없어요."
            page={members?.page ?? 0}
            totalPages={members?.totalPages ?? 1}
            onPage={setPage}
            notice={actionError ? <Notice tone="error">{actionError}</Notice> : null}
          />
        ) : null}

        {tab === 'evidence' ? (
          <AdminTablePanel
            title="성취 증빙 검수"
            description="회원이 올린 수상 증빙을 내려받아 확인하고 승인 · 반려해요. 반려할 때는 사유를 남겨야 해요."
            columns={evidenceColumns}
            rows={evidences?.items ?? []}
            rowKey={(row) => String(row.goalId)}
            loading={loading}
            error={loadError}
            emptyMessage={`${EVIDENCE_STATUS_LABELS[evidenceStatus]} 상태인 증빙이 없어요.`}
            notice={actionError ? <Notice tone="error">{actionError}</Notice> : null}
            page={evidences?.page ?? 0}
            totalPages={evidences?.totalPages ?? 1}
            onPage={setPage}
            toolbar={
              <RadioChipGroup
                options={EVIDENCE_STATUSES.map((value) => EVIDENCE_STATUS_LABELS[value])}
                value={EVIDENCE_STATUS_LABELS[evidenceStatus]}
                onChange={(label) => {
                  const next = EVIDENCE_STATUSES.find((value) => EVIDENCE_STATUS_LABELS[value] === label);
                  if (!next) return;
                  setEvidenceStatus(next);
                  setPage(0);
                }}
              />
            }
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

      <Modal
        open={rejecting !== null}
        title="증빙 반려"
        description={
          rejecting
            ? `${rejecting.ownerName} 님의 '${rejecting.title}' 증빙을 반려해요.`
            : undefined
        }
        confirmLabel={working ? '반려하는 중…' : '반려'}
        confirmVariant="danger"
        cancelLabel="취소"
        onConfirm={() => {
          if (!rejecting || working) return;
          if (!rejectNote.trim()) {
            setRejectError('반려 사유를 입력해 주세요.');
            return;
          }
          submitReview(rejecting, 'REJECTED', rejectNote.trim());
        }}
        onClose={() => {
          if (working) return;
          setRejecting(null);
          setRejectError('');
        }}
      >
        <FormGroup
          label="반려 사유"
          required
          hint="올린 사람에게만 보여요. 무엇을 고쳐 다시 올려야 하는지 적어주세요."
          error={rejectError || undefined}
        >
          <TextAreaField
            rows={4}
            value={rejectNote}
            onChange={(event) => {
              setRejectNote(event.target.value);
              if (rejectError) setRejectError('');
            }}
            placeholder="예) 확인서가 흐릿해서 대회명이 보이지 않아요."
          />
        </FormGroup>
      </Modal>

      <Modal
        open={suspending !== null}
        title="회원 정지"
        description={suspending ? `${suspending.name} 님을 정지해요.` : undefined}
        confirmLabel={working ? '정지하는 중…' : '정지'}
        confirmVariant="danger"
        cancelLabel="취소"
        onConfirm={submitSuspend}
        onClose={() => {
          if (working) return;
          setSuspending(null);
          setSuspendError('');
        }}
      >
        <FormGroup
          label="정지 사유"
          required
          hint="운영 기록으로 남아요. 어떤 규정을 어겼는지 적어주세요."
          error={suspendError || undefined}
        >
          <TextAreaField
            rows={3}
            value={suspendReason}
            onChange={(event) => {
              setSuspendReason(event.target.value);
              if (suspendError) setSuspendError('');
            }}
            placeholder="예) 커뮤니티 가이드라인 위반(허위 지원 반복)"
          />
        </FormGroup>
      </Modal>

      <Modal
        open={detailOf !== null}
        title={detailOf ? detailOf.title : '성취 내용'}
        description={detailOf ? `${detailOf.ownerName} 님이 올린 증빙이에요.` : undefined}
        onClose={() => {
          setDetailOf(null);
          setGoalDetail(null);
        }}
      >
        {detailError ? (
          <Notice tone="error">{detailError}</Notice>
        ) : !goalDetail ? (
          <p className="form-hint">불러오는 중이에요…</p>
        ) : (
          <dl className="admin-detail">
            <div>
              <dt>대회명</dt>
              <dd>{goalDetail.detail.title ?? '-'}</dd>
            </div>
            <div>
              <dt>참가 형태</dt>
              <dd>
                {goalDetail.detail.isTeam == null
                  ? '-'
                  : goalDetail.detail.isTeam
                    ? '팀 참가'
                    : '개인 참가'}
              </dd>
            </div>
            <div>
              <dt>수상 결과</dt>
              <dd>{goalDetail.detail.result ?? '-'}</dd>
            </div>
            <div>
              <dt>수상일</dt>
              <dd>{dateOnly(goalDetail.detail.awardDate ?? null)}</dd>
            </div>
            <div>
              <dt>성취 상태</dt>
              <dd>{GOAL_STATUS_LABELS[goalDetail.status] ?? goalDetail.status}</dd>
            </div>
            <div>
              <dt>등록일</dt>
              <dd>{dateOnly(goalDetail.createDate)}</dd>
            </div>
            <div>
              <dt>대회 링크</dt>
              <dd>
                {/*
                  회원이 자유롭게 적는 값이고 서버가 검증하지 않는다.
                  http·https 가 아니면 링크로 걸지 않고 적힌 그대로만 보여준다.
                */}
                {httpUrlOrNull(goalDetail.detail.contestUrl) ? (
                  <a
                    className="admin-link"
                    href={goalDetail.detail.contestUrl}
                    target="_blank"
                    rel="noreferrer noopener"
                  >
                    {goalDetail.detail.contestUrl} ↗
                  </a>
                ) : (
                  (goalDetail.detail.contestUrl ?? '-')
                )}
              </dd>
            </div>
            <div>
              <dt>증빙 파일</dt>
              <dd>
                {detailOf?.fileName ?? '-'}
                {detailOf?.mimeType ? ` · ${detailOf.mimeType}` : ''}
                {detailOf ? ` · ${fileSize(detailOf.size)}` : ''}
              </dd>
            </div>
            <div>
              <dt>검수 상태</dt>
              <dd>{detailOf ? EVIDENCE_STATUS_LABELS[detailOf.status] : '-'}</dd>
            </div>
            {detailOf?.reviewNote ? (
              <div>
                <dt>반려 사유</dt>
                <dd>{detailOf.reviewNote}</dd>
              </div>
            ) : null}
          </dl>
        )}
      </Modal>

      <Modal
        open={historyOf !== null}
        title={historyOf ? `${historyOf.name} 님의 이력` : '회원 이력'}
        description={historyOf?.email}
        onClose={() => {
          setHistoryOf(null);
          setHistory(null);
        }}
      >
        {historyError ? (
          <Notice tone="error">{historyError}</Notice>
        ) : !history ? (
          <p className="form-hint">불러오는 중이에요…</p>
        ) : (
          <div className="admin-history">
            <section>
              <h4>만든 파티 ({history.partyHistory.ownedParties.length})</h4>
              {history.partyHistory.ownedParties.length ? (
                <ul>
                  {history.partyHistory.ownedParties.map((party) => (
                    <li key={party.partyId}>
                      <b>{party.partyName}</b>
                      <span>
                        {PARTY_STATUS_LABELS[party.status as PartyStatus] ?? party.status}
                        {party.hidden ? ' · 숨김' : ''} · {dateOnly(party.createDate)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="form-hint">만든 파티가 없어요.</p>
              )}
            </section>

            <section>
              <h4>지원한 파티 ({history.partyHistory.appliedParties.length})</h4>
              {history.partyHistory.appliedParties.length ? (
                <ul>
                  {history.partyHistory.appliedParties.map((party) => (
                    <li key={`${party.partyId}-${party.appliedAt}`}>
                      <b>{party.partyName}</b>
                      <span>
                        {party.position} · {party.applicationStatus} · {dateOnly(party.appliedAt)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="form-hint">지원한 파티가 없어요.</p>
              )}
            </section>

            <section>
              <h4>성취 ({history.achievements.length})</h4>
              {history.achievements.length ? (
                <ul>
                  {history.achievements.map((goal) => (
                    <li key={goal.id}>
                      <b>{goal.title}</b>
                      <span>
                        {GOAL_TYPE_LABELS[goal.type as GoalType] ?? goal.type} ·{' '}
                        {GOAL_STATUS_LABELS[goal.status as GoalStatus] ?? goal.status} ·{' '}
                        {dateOnly(goal.createDate)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="form-hint">등록한 성취가 없어요.</p>
              )}
            </section>
          </div>
        )}
      </Modal>

      {dialog}
    </div>
  );
}

interface AdminTablePanelProps<T> {
  title: string;
  description: string;
  addLabel?: string;
  /** 검색 입력을 쓰지 않는 패널은 생략한다 */
  placeholder?: string;
  search?: string;
  onSearch?: (value: string) => void;
  /** 검색 입력 옆에 놓을 추가 조작(상태 필터 등) */
  toolbar?: ReactNode;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string;
  loading?: boolean;
  error?: string;
  /** 표 위에 띄우는 안내·오류. 목록은 그대로 두고 실패만 알릴 때 쓴다 */
  notice?: ReactNode;
  emptyMessage?: string;
  /** 서버와 같은 0-based 페이지. Pagination 은 1-based 라 여기서 맞춘다 */
  page?: number;
  totalPages?: number;
  onPage?: (page: number) => void;
}

function AdminTablePanel<T>({
  title,
  description,
  addLabel,
  placeholder,
  search,
  onSearch,
  toolbar,
  columns,
  rows,
  rowKey,
  loading,
  error,
  notice,
  emptyMessage,
  page = 0,
  totalPages = 1,
  onPage,
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
      {onSearch || toolbar ? (
        <div className="admin-toolbar">
          {onSearch ? (
            <input
              type="text"
              className="admin-search"
              placeholder={placeholder}
              aria-label={placeholder ?? '검색'}
              value={search ?? ''}
              onChange={(event) => onSearch(event.target.value)}
            />
          ) : null}
          {toolbar}
        </div>
      ) : null}
      {notice}
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={rowKey}
        loading={loading}
        error={error}
        emptyMessage={emptyMessage}
      />
      {onPage ? (
        <Pagination page={page + 1} totalPages={totalPages} onChange={(next) => onPage(next - 1)} />
      ) : null}
    </section>
  );
}
