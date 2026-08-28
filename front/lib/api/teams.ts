import type { ChecklistItem, ThreadComment, TeamCommit, TeamSpace } from '@/lib/types';
import { MOCK_TEAM_COMMITS, MOCK_TEAM_REPOSITORY, MOCK_TEAM_SPACE } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

/** GET /teams/{partyId} */
export async function fetchTeamSpace(partyId: string): Promise<TeamSpace> {
  if (USE_MOCK) return mockResponse({ ...MOCK_TEAM_SPACE, partyId });
  return http.get<TeamSpace>(`/teams/${partyId}`);
}

/** GET /teams/{partyId}/repository */
export async function fetchTeamRepository(partyId: string): Promise<string> {
  if (USE_MOCK) return mockResponse(MOCK_TEAM_REPOSITORY);
  return http.get<string>(`/teams/${partyId}/repository`);
}

/**
 * GET /teams/{partyId}/commits — GitHub 웹훅으로 수집한 커밋 내역.
 * 저장 스키마가 정해지기 전까지는 데모 값을 최신순으로 돌려준다.
 */
export async function fetchTeamCommits(partyId: string): Promise<TeamCommit[]> {
  if (USE_MOCK) return mockResponse(MOCK_TEAM_COMMITS);
  return http.get<TeamCommit[]>(`/teams/${partyId}/commits`);
}

/** POST /teams/{partyId}/checklist */
export async function createChecklistItem(
  partyId: string,
  payload: { content: string; assignee: string | null },
): Promise<ChecklistItem> {
  if (USE_MOCK) {
    return mockResponse({
      id: `chk-${Date.now()}`,
      content: payload.content,
      state: 'open',
      assignee: payload.assignee,
      approvals: 0,
      quorum: 1,
    });
  }
  return http.post<ChecklistItem>(`/teams/${partyId}/checklist`, payload);
}

/** PUT /teams/{partyId}/checklist/{itemId} */
export async function updateChecklistItem(
  partyId: string,
  itemId: string,
  payload: { content: string; assignee: string | null },
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.put<void>(`/teams/${partyId}/checklist/${itemId}`, payload);
}

/** DELETE /teams/{partyId}/checklist/{itemId} */
export async function deleteChecklistItem(partyId: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.delete<void>(`/teams/${partyId}/checklist/${itemId}`);
}

/** POST /teams/{partyId}/checklist/{itemId}/done — 개인 TODO 항목 완료 처리 */
export async function completeChecklistItem(partyId: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>(`/teams/${partyId}/checklist/${itemId}/done`);
}

/**
 * POST /teams/{partyId}/commits/{commitId}/approve — 커밋 동료 승인.
 * 작성자 본인은 자기 커밋을 승인할 수 없고, 정족수를 채우면 approved 로 바뀐다.
 */
export async function approveCommit(
  partyId: string,
  commitId: string,
): Promise<{ approvals: number; approvalState: 'pending' | 'approved' }> {
  if (USE_MOCK) {
    const commit = MOCK_TEAM_COMMITS.find((item) => item.id === commitId);
    const approvals = (commit?.approvals ?? 0) + 1;
    return mockResponse({
      approvals,
      approvalState: approvals >= (commit?.quorum ?? 1) ? 'approved' : 'pending',
    });
  }
  return http.post(`/teams/${partyId}/commits/${commitId}/approve`);
}

/**
 * POST /teams/{partyId}/commits/{commitId}/comments
 * parentId 를 주면 그 원댓글의 답글이 된다 — 답글의 답글은 허용하지 않는다 (기획서 3.8).
 */
export async function createCommitComment(
  partyId: string,
  commitId: string,
  payload: { content: string; parentId?: string },
): Promise<ThreadComment> {
  if (USE_MOCK) {
    return mockResponse({
      id: `c-${Date.now()}`,
      authorName: '정하늘',
      authorInitial: '정',
      content: payload.content,
      createdAt: '방금',
      replies: [],
    });
  }
  return http.post<ThreadComment>(`/teams/${partyId}/commits/${commitId}/comments`, payload);
}

/** PUT /teams/{partyId}/commits/{commitId}/comments/{commentId} */
export async function updateCommitComment(
  partyId: string,
  commitId: string,
  commentId: string,
  content: string,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.put<void>(`/teams/${partyId}/commits/${commitId}/comments/${commentId}`, { content });
}

/** DELETE /teams/{partyId}/commits/{commitId}/comments/{commentId} */
export async function deleteCommitComment(
  partyId: string,
  commitId: string,
  commentId: string,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.delete<void>(`/teams/${partyId}/commits/${commitId}/comments/${commentId}`);
}

/** POST /teams/{partyId}/finish — 파티 진행 완료 */
export async function finishParty(partyId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>(`/teams/${partyId}/finish`);
}
