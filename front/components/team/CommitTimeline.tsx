'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { CommentThread, CommentToggleButton, countComments } from '@/components/comment/CommentThread';
import { ProgressBar } from '@/components/ui/ProgressBar';
import {
  approveCommit,
  createCommitComment,
  deleteCommitComment,
  updateCommitComment,
} from '@/lib/api';
import type { ThreadComment, TeamCommit } from '@/lib/types';

interface CommitTimelineProps {
  partyId: string;
  commits: TeamCommit[];
  threads: Record<string, ThreadComment[]>;
  currentUser: string;
  quorum: number;
}

/**
 * 팀 커밋 내역 — GitHub 웹훅(push)으로 들어온 커밋을 날짜별로 묶어 보여준다.
 *
 * 진행 기록은 수동 체크리스트 대신 이 커밋 목록으로 남기고,
 * 동료 승인(기획서 2.7 동료인증)도 커밋 단위로 이뤄진다.
 * 성취 연혁(.achv-*)과 같은 타임라인 형태를 쓴다.
 */
export function CommitTimeline({
  partyId,
  commits: initialCommits,
  threads: initialThreads,
  currentUser,
  quorum,
}: CommitTimelineProps) {
  const [commits, setCommits] = useState(initialCommits);
  const [threads, setThreads] = useState(initialThreads);
  const [openThreadId, setOpenThreadId] = useState<string | null>(null);

  const approvedCount = commits.filter((commit) => commit.approvalState === 'approved').length;

  const approve = async (commit: TeamCommit) => {
    const approvals = commit.approvals + 1;
    setCommits((prev) =>
      prev.map((row) =>
        row.id === commit.id
          ? {
              ...row,
              approvals,
              approvers: [...row.approvers, currentUser],
              approvalState: approvals >= row.quorum ? 'approved' : 'pending',
            }
          : row,
      ),
    );
    await approveCommit(partyId, commit.id);
  };

  if (commits.length === 0) {
    return (
      <p className="notif-empty">
        아직 수집된 커밋이 없어요. 저장소에 푸시하면 여기에 자동으로 쌓입니다.
      </p>
    );
  }

  // 날짜별 그룹 — 데이터는 최신순으로 내려온다
  const groups: [string, TeamCommit[]][] = [];
  commits.forEach((commit) => {
    const last = groups[groups.length - 1];
    if (last && last[0] === commit.date) last[1].push(commit);
    else groups.push([commit.date, [commit]]);
  });

  return (
    <>
      <ProgressBar done={approvedCount} total={commits.length} />
      <p className="checklist-quorum">
        승인 정족수 <b>{quorum}명</b> · 커밋 작성자 본인은 자기 커밋을 승인할 수 없어요. 정족수를
        채우면 승인 완료로 기록됩니다.
      </p>

      <div className="achv-timeline">
        {groups.map(([date, items]) => (
          <div key={date} className="achv-year">
            <div className="achv-year-head">
              <span className="achv-year-label">{date}</span>
              <span className="achv-year-count">{items.length}건</span>
              <span className="achv-year-rule" />
            </div>

            <div className="achv-track">
              {items.map((commit) => {
                const comments = threads[commit.id] ?? [];
                const mine = commit.authorName === currentUser;
                return (
                  <div
                    key={commit.id}
                    className="achv-item commit-item"
                    data-approval={commit.approvalState}
                  >
                    <div className="achv-item-top">
                      <span className="achv-date">{commit.time}</span>
                      <span className="commit-sha">{commit.sha}</span>
                      <span className="status-badge">{commit.branch}</span>
                      {commit.approvalState === 'approved' ? (
                        <span className="status-badge achieved">
                          승인 완료 · {commit.approvers.join(', ')}
                        </span>
                      ) : (
                        <span className="status-badge progress">
                          승인 {commit.approvals}/{commit.quorum}
                        </span>
                      )}
                    </div>

                    <h5>{commit.message}</h5>

                    <div className="commit-meta">
                      {/* 커밋 작성자는 githubUsername 으로 크루온 회원과 매칭한다 */}
                      <span className="commit-author">
                        <span className="mini-avatar">{commit.authorInitial}</span>
                        {commit.memberId ? (
                          <Link
                            href={`/profile/${commit.memberId}`}
                            className="commit-author-name"
                          >
                            {commit.authorName}
                          </Link>
                        ) : (
                          <span className="commit-author-name is-unlinked">
                            {commit.authorName}
                          </span>
                        )}
                        <span className="commit-handle">@{commit.githubUsername}</span>
                      </span>

                      <span className="commit-diff">
                        <span className="add">+{commit.additions}</span>
                        <span className="del">-{commit.deletions}</span>
                        <span className="files">파일 {commit.changedFiles}</span>
                      </span>

                      <a
                        className="commit-link"
                        href={commit.url}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <Icon name="i-external" />
                        GitHub
                      </a>
                    </div>

                    <div className="commit-actions">
                      <CommentToggleButton
                        count={countComments(comments)}
                        open={openThreadId === commit.id}
                        onToggle={() =>
                          setOpenThreadId(openThreadId === commit.id ? null : commit.id)
                        }
                      />
                      {commit.approvalState === 'approved' ? null : mine ? (
                        <span className="wait-note">
                          동료 승인 대기 ({commit.approvals}/{commit.quorum})
                        </span>
                      ) : (
                        <button
                          type="button"
                          className="btn-mini"
                          onClick={() => approve(commit)}
                        >
                          승인
                        </button>
                      )}
                    </div>

                    {openThreadId === commit.id ? (
                      <CommentThread
                        comments={comments}
                        currentUser={currentUser}
                        onChange={(next) =>
                          setThreads((prev) => ({ ...prev, [commit.id]: next }))
                        }
                        onCreate={(content, parentId) =>
                          createCommitComment(partyId, commit.id, { content, parentId })
                        }
                        onUpdate={(commentId, content) =>
                          updateCommitComment(partyId, commit.id, commentId, content)
                        }
                        onDelete={(commentId) => deleteCommitComment(partyId, commit.id, commentId)}
                      />
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
