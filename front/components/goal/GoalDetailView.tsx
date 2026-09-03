import type { ReactNode } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { SourceBadge, Tag } from '@/components/ui/Tag';
import { GoalOwnerTools } from './GoalOwnerTools';
import {
  GOAL_POSITION_LABELS,
  evidenceFilesOf,
  formatFileSize,
  type GoalDetailResponse,
  type PartyPrResponse,
  type TodoContextResponse,
} from '@/lib/api/goals';
import {
  GOAL_STATUS_LABELS,
  GOAL_TYPE_LABELS,
  PARTY_STATUS_LABELS,
  todoCategoryLabel,
} from '@/lib/constants';

/** yyyy-MM-dd 또는 ISO 문자열 → 2026.08.01 */
function formatDate(value?: string | null): string {
  if (!value) return '';
  return value.slice(0, 10).replace(/-/g, '.');
}

/** ISO 문자열 → 2026.08.20 23:59 */
function formatDateTime(value?: string | null): string {
  if (!value) return '';
  const date = formatDate(value);
  const time = value.slice(11, 16);
  return time ? `${date} ${time}` : date;
}

/** 성취 목록(.achv-item)에서 쓰는 상태 배지 색을 그대로 맞춘다 */
function statusBadgeClass(status: GoalDetailResponse['status']): string {
  if (status === 'ACHIEVED') return 'status-badge achieved';
  if (status === 'IN_PROGRESS') return 'status-badge progress';
  return 'status-badge';
}

/**
 * PR 상태 문구.
 * GitHub 의 state 는 open/closed 둘뿐이라 머지·초안 여부를 합쳐서 보여준다.
 */
function prState(pr: PartyPrResponse): { label: string; tone: string } {
  if (pr.merged) return { label: '머지됨', tone: 'achieved' };
  if (pr.state === 'closed') return { label: '닫힘', tone: '' };
  if (pr.draft) return { label: '초안', tone: '' };
  return { label: '열림', tone: 'progress' };
}

type InfoRow = [label: string, value: ReactNode];

/** 값이 없는 행은 넣지 않는다 — 타입마다 채워지는 필드가 다르다 */
function InfoList({ rows }: { rows: InfoRow[] }) {
  const visible = rows.filter(([, value]) => value !== null && value !== undefined && value !== '');
  if (visible.length === 0) return null;

  return (
    <dl className="goal-info">
      {visible.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** 화면 제목 — PROJECT 는 전시 게시 전까지 title 이 비어 있어 파티명을 대신 쓴다 */
function goalTitle(goal: GoalDetailResponse): string {
  const { detail, project, type } = goal;
  if (type === 'PROJECT') return detail.title ?? project?.partyName ?? '참여한 파티';
  if (type === 'CONTEST') return detail.title ?? '수상·대회';
  return detail.title ?? '체크리스트';
}

interface GoalDetailViewProps {
  goal: GoalDetailResponse;
  /** 보고 있는 사람의 회원 id. 미로그인이면 없음 */
  viewerId?: string;
}

/**
 * 성취 상세 화면 (GET /api/v1/goals/{goalId}).
 *
 * 성취 자체는 색인에 가까워서, 실제 내용은 타입에 따라 다른 곳에 있다.
 * - PROJECT   : 파티가 내용을 갖는다. 응답의 project 블록으로 파티 정보와 PR 목록을 보여준다
 * - CONTEST   : 성취가 직접 내용을 갖는다. 증빙은 메타데이터만 온다
 * - CHECKLIST : 성취가 직접 내용을 갖는다
 */
export function GoalDetailView({ goal, viewerId }: GoalDetailViewProps) {
  const { project } = goal;
  const isOwner = viewerId != null && viewerId === String(goal.ownerId);

  return (
    <main>
      <div className="board-wrap container">
        <BackLink label="뒤로" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <div className="tag-row goal-badges">
                  <Tag>{GOAL_TYPE_LABELS[goal.type]}</Tag>
                  <span className={statusBadgeClass(goal.status)}>
                    {GOAL_STATUS_LABELS[goal.status]}
                  </span>
                  <SourceBadge source={goal.source} />
                </div>
              </div>

              <h1 className="detail-title">{goalTitle(goal)}</h1>

              <p className="goal-owner-line">
                <Link href={`/profile/${goal.ownerId}`}>{goal.ownerName}</Link>
                <span className="goal-dot">·</span>
                등록 {formatDate(goal.createDate)}
              </p>

              {/* 자동기록 성취는 사용자가 만든 게 아니라는 걸 화면에서도 알려준다 (기획서 2.5) */}
              {goal.source === 'PLATFORM_VERIFIED' ? (
                <p className="goal-note">
                  파티 활동에 따라 플랫폼이 자동으로 남긴 기록이에요. 직접 수정하거나 삭제할 수
                  없어요.
                </p>
              ) : null}

              {goal.type === 'PROJECT' ? (
                <ProjectSection goal={goal} isOwner={isOwner} />
              ) : goal.type === 'CONTEST' ? (
                <ContestSection goal={goal} />
              ) : (
                <ChecklistSection goal={goal} />
              )}
            </>
          }
          side={
            <>
              {isOwner ? <GoalOwnerTools goal={goal} /> : null}

              <SideCard title="성취 정보">
                <InfoList
                  rows={[
                    ['유형', GOAL_TYPE_LABELS[goal.type]],
                    ['상태', GOAL_STATUS_LABELS[goal.status]],
                    ['등록일', formatDate(goal.createDate)],
                    ['최근 수정', formatDate(goal.modifyDate)],
                  ]}
                />
              </SideCard>

              {project && (isOwner || project.githubRepoUrl) ? (
                <SideCard title="바로가기">
                  {/* 팀 스페이스는 파티원에게만 열려 있어(기획서 9.3) 남의 성취에서는 걸지 않는다 */}
                  {isOwner ? (
                    <Link className="card-link" href={`/party/${project.partyId}/team`}>
                      팀 스페이스 열기 →
                    </Link>
                  ) : null}
                  {project.githubRepoUrl ? (
                    <a
                      className="card-link"
                      href={project.githubRepoUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Icon name="i-github" /> 저장소 열기 ↗
                    </a>
                  ) : null}
                </SideCard>
              ) : null}

            </>
          }
        />
      </div>
    </main>
  );
}

/** PROJECT — 내용은 전부 파티에 있어서 project 블록을 펼쳐 보여준다 */
function ProjectSection({ goal, isOwner }: { goal: GoalDetailResponse; isOwner: boolean }) {
  const { detail, project } = goal;

  if (!project) {
    return (
      <Block title="참여한 파티" className="block-spaced">
        <p className="goal-empty">연결된 파티 정보를 불러오지 못했어요.</p>
      </Block>
    );
  }

  const period = detail.startDate
    ? `${formatDate(detail.startDate)} ~ ${detail.endDate ? formatDate(detail.endDate) : '진행중'}`
    : '';

  return (
    <>
      <Block title="참여한 파티" className="block-spaced">
        <div className="contest-link-card">
          <div>
            <p className="clc-label">{PARTY_STATUS_LABELS[project.partyStatus]}</p>
            <h4>{project.partyName}</h4>
            <p className="clc-sub">{project.title}</p>
          </div>
          <Link className="card-link" href={`/party/${project.partyId}`}>
            파티 보기 →
          </Link>
        </div>

        <InfoList
          rows={[
            [
              '맡은 포지션',
              // 파티장은 지원 절차가 없어 포지션이 비어 있다
              project.myPositionType
                ? GOAL_POSITION_LABELS[project.myPositionType]
                : project.partyOwner
                  ? '파티장'
                  : '',
            ],
            ['역할', project.partyOwner ? '파티장' : '팀원'],
            ['참여 기간', period],
            ['모집 마감', formatDateTime(project.deadline)],
            [
              '저장소',
              project.githubRepoUrl ? (
                <a href={project.githubRepoUrl} target="_blank" rel="noopener noreferrer">
                  {project.githubRepoUrl.replace('https://github.com/', '')} ↗
                </a>
              ) : (
                '등록된 저장소 없음'
              ),
            ],
          ]}
        />

        {detail.result ? <p className="detail-desc">{detail.result}</p> : null}
      </Block>

      <Block title="동기화된 PR" description="파티 저장소에서 수집한 PR 목록이에요.">
        {project.pullRequests.length > 0 ? (
          <ul className="goal-pr-list">
            {project.pullRequests.map((pr) => {
              const { label, tone } = prState(pr);
              return (
                <li key={pr.id} className="goal-pr-item">
                  <div className="goal-pr-top">
                    <span className="goal-pr-num">#{pr.number}</span>
                    <span className={tone ? `status-badge ${tone}` : 'status-badge'}>{label}</span>
                    <span className="goal-pr-branch">
                      {pr.headBranch} → {pr.baseBranch}
                    </span>
                  </div>

                  <a
                    className="goal-pr-title"
                    href={pr.htmlUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {pr.title}
                    <Icon name="i-external" />
                  </a>

                  <p className="goal-pr-meta">
                    {/* GitHub 로그인명이라 크루온 회원 프로필로는 아직 연결되지 않는다 */}
                    <span className="goal-pr-author">@{pr.authorLogin}</span>
                    <span className="goal-dot">·</span>
                    {pr.merged
                      ? `머지 ${formatDate(pr.mergedAt)}`
                      : `등록 ${formatDate(pr.openedAt)}`}
                  </p>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="goal-empty">
            {isOwner
              ? '아직 동기화된 PR이 없어요. 저장소에 PR을 올리면 여기에 쌓입니다.'
              : '파티 진행 기록은 파티원에게만 열려 있어 여기서는 보이지 않아요.'}
          </p>
        )}
      </Block>
    </>
  );
}

/** CONTEST — 대회 정보와 증빙 메타데이터 */
function ContestSection({ goal }: { goal: GoalDetailResponse }) {
  const { detail } = goal;

  /*
   * 증빙 파일 목록.
   * 서버는 아직 한 건 분량의 필드(evidenceFileName·evidenceMimeType…)만 내려주지만,
   * 여러 건(detail.evidences)으로 늘어나도 화면이 그대로 받도록 목록으로 다룬다.
   */
  const evidenceFiles = evidenceFilesOf(detail);

  return (
    <>
      <Block title="대회 정보" className="block-spaced">
        <InfoList
          rows={[
            ['대회명', detail.title],
            ['참가 형태', detail.isTeam == null ? '' : detail.isTeam ? '팀 참가' : '개인 참가'],
            ['수상 결과', detail.result],
            ['수상일', formatDate(detail.awardDate)],
            [
              '대회 링크',
              detail.contestUrl ? (
                <a href={detail.contestUrl} target="_blank" rel="noopener noreferrer">
                  {detail.contestUrl} ↗
                </a>
              ) : (
                ''
              ),
            ],
            // 자기신고는 외부 대회를 적는 것이라 보통 비어 있다. 크루온 대회와 이어졌을 때만 보여준다
            [
              '크루온 공모전',
              detail.targetContestId ? (
                <Link href={`/contests/${detail.targetContestId}`}>공모전 상세 보기 →</Link>
              ) : (
                ''
              ),
            ],
          ]}
        />
      </Block>

      <Block title="증빙 자료">
        {evidenceFiles.length > 0 ? (
          <>
            <ul className="goal-file-list">
              {evidenceFiles.map((file) => {
                const meta = [file.mimeType, formatFileSize(file.size)].filter(Boolean).join(' · ');
                return (
                  <li key={`${file.fileName}:${file.size ?? ''}`}>
                    <span className="goal-file-name">{file.fileName}</span>
                    {meta ? <span className="goal-file-type">{meta}</span> : null}
                  </li>
                );
              })}
            </ul>
            {/* 파일 자체는 Object Storage 에 있고 응답에는 메타데이터만 온다.
                내려받기 URL 발급 API가 붙기 전까지는 등록 여부만 보여준다. */}
            <p className="goal-note">
              증빙 파일 {evidenceFiles.length}건이 등록되어 있어요. 내려받기는 준비 중이에요.
            </p>
          </>
        ) : (
          <p className="goal-empty">등록된 증빙 자료가 없어요.</p>
        )}
      </Block>
    </>
  );
}

/** CHECKLIST — 스스로 정한 목표와, 연결했다면 그 진행 과정 */
function ChecklistSection({ goal }: { goal: GoalDetailResponse }) {
  const { detail, todo } = goal;

  return (
    <>
      <Block title="목표 내용" className="block-spaced">
        <InfoList
          rows={[
            ['목표', detail.title],
            ['목표일', formatDate(detail.targetDate)],
          ]}
        />
        {detail.memo ? (
          <p className="detail-desc">{detail.memo}</p>
        ) : (
          <p className="goal-empty">적어둔 메모가 없어요.</p>
        )}
      </Block>

      <TodoProgressBlock todo={todo} />
    </>
  );
}

/**
 * 연결된 개인 TODO의 진행 과정 — 마이페이지 연혁과 같은 타임라인으로 보여준다.
 *
 * 서버가 완료한 항목만 해낸 순서대로 내려준다. 미완료 항목과 진행률은 오지 않는다.
 */
function TodoProgressBlock({ todo }: { todo?: TodoContextResponse }) {
  if (!todo) {
    return (
      <Block title="진행 과정">
        <p className="goal-empty">
          연결된 개인 TODO가 없어요. 성취를 수정해 개인 TODO를 연결하면 진행 과정이 여기에 쌓입니다.
        </p>
      </Block>
    );
  }

  return (
    <Block
      title="진행 과정"
      description={`개인 TODO '${todo.title}'에 쌓은 할 일이에요.`}
    >
      {/* 진행률(2/4 같은 표기)은 두지 않는다. 소유자 개인의 관리 지표라 성취 상세가 보여줄 것이 아니다 */}
      <div className="goal-todo-summary">
        <span className="status-badge">{todoCategoryLabel(todo.category)}</span>
      </div>

      {/* TODO 메모는 목록 전체에 대한 말이라 항목 위에 둔다 - 아래에 두면 마지막 항목 설명처럼 읽힌다 */}
      {todo.memo ? <p className="goal-todo-memo">{todo.memo}</p> : null}

      {todo.items.length > 0 ? (
        <div className="achv-track goal-todo-track">
          {todo.items.map((item) => (
            <div key={item.id} className="achv-item" data-status="달성">
              <div className="achv-item-top">
                <span className="achv-date">{formatDate(item.doneAt)}</span>
              </div>
              <h5>{item.content}</h5>
            </div>
          ))}
        </div>
      ) : (
        <p className="goal-empty">아직 끝낸 할 일이 없어요.</p>
      )}
    </Block>
  );
}
