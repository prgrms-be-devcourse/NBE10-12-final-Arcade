/**
 * 성취 상세(GET /goals/{goalId}) 데모 데이터.
 *
 * 백엔드 GoalDetailResponseDto 를 그대로 흉내 낸다.
 * 타입별로 응답 모양이 달라(@JsonInclude NON_NULL) 세 종류를 모두 둔다.
 */
import type { GoalDetailResponse } from '@/lib/api/goals';

const PROJECT_GOAL: GoalDetailResponse = {
  id: 1,
  ownerId: 1,
  ownerName: '김하늘',
  type: 'PROJECT',
  status: 'IN_PROGRESS',
  source: 'PLATFORM_VERIFIED',
  sourcePartyId: 10,
  viewCount: 2064,
  detail: {
    // 전시 게시(PARTY_SHOWCASE) 전이라 title·result 는 아직 비어 있다
    positionType: 'BACK',
    startDate: '2026-08-01',
  },
  project: {
    partyId: 10,
    partyName: '페이브릿지 해커톤 도전팀',
    title: '정산 자동화 API 만들 백엔드·프론트 구합니다',
    partyStatus: 'IN_PROGRESS',
    deadline: '2026-08-20T23:59:00',
    githubRepoUrl: 'https://github.com/crewon/settlement-api',
    myPositionType: 'BACK',
    partyOwner: false,
    pullRequests: [
      {
        id: 1,
        githubPrId: 900001,
        number: 42,
        title: '정산 매칭 로직 캐시 레이어 추가',
        htmlUrl: 'https://github.com/crewon/settlement-api/pull/42',
        state: 'closed',
        authorLogin: 'skyjeong',
        draft: false,
        merged: true,
        baseBranch: 'develop',
        headBranch: 'feat/settle-cache',
        openedAt: '2026-08-18T09:12:00+09:00',
        closedAt: '2026-08-19T14:02:00+09:00',
        mergedAt: '2026-08-19T14:02:00+09:00',
        githubUpdatedAt: '2026-08-19T14:02:00+09:00',
      },
      {
        id: 2,
        githubPrId: 900002,
        number: 45,
        title: '랭킹 API 페이지네이션',
        htmlUrl: 'https://github.com/crewon/settlement-api/pull/45',
        state: 'open',
        authorLogin: 'skyjeong',
        draft: true,
        merged: false,
        baseBranch: 'develop',
        headBranch: 'feat/rank-paging',
        openedAt: '2026-08-24T11:40:00+09:00',
        closedAt: null,
        mergedAt: null,
        githubUpdatedAt: '2026-08-25T10:05:00+09:00',
      },
    ],
  },
  createDate: '2026-08-01T10:00:00',
  modifyDate: '2026-08-25T10:05:00',
};

const CONTEST_GOAL: GoalDetailResponse = {
  id: 2,
  ownerId: 1,
  ownerName: '김하늘',
  type: 'CONTEST',
  status: 'ACHIEVED',
  source: 'SELF_REPORTED',
  sourcePartyId: null,
  viewCount: 318,
  detail: {
    title: '2025 공공데이터 활용 공모전',
    isTeam: true,
    result: '우수상',
    awardDate: '2025-09-12',
    contestUrl: 'https://www.data.go.kr/contest/2025',
    // 서버가 아직 한 건만 보관해서 예전 필드에는 첫 파일이 담긴다
    evidenceStorageKey: 'goal/2/evidence/award.pdf',
    evidenceFileName: '수상확인서.pdf',
    evidenceMimeType: 'application/pdf',
    evidenceSize: 284_915,
    // 여러 건이 열렸을 때의 모양. 화면은 이 목록을 먼저 본다
    evidences: [
      {
        storageKey: 'goal/2/evidence/award.pdf',
        fileName: '수상확인서.pdf',
        mimeType: 'application/pdf',
        size: 284_915,
      },
      {
        storageKey: 'goal/2/evidence/result.png',
        fileName: '결과발표화면.png',
        mimeType: 'image/png',
        size: 1_204_330,
      },
    ],
  },
  createDate: '2025-09-13T20:11:00',
  modifyDate: '2025-09-13T20:11:00',
};

const CHECKLIST_GOAL: GoalDetailResponse = {
  id: 3,
  ownerId: 1,
  ownerName: '김하늘',
  type: 'CHECKLIST',
  status: 'WANT',
  source: 'SELF_REPORTED',
  sourcePartyId: null,
  viewCount: 12,
  detail: {
    title: '정보처리기사 취득',
    memo: '필기 2월, 실기 5월 목표. 실기는 SQL·알고리즘 파트를 먼저 정리하기.',
    targetDate: '2026-05-30',
    todoId: 1,
  },
  // 연결된 개인 TODO — 상세의 '진행 과정' 타임라인이 이 값으로 그려진다
  todo: {
    todoId: 1,
    title: '정보처리기사 실기 준비',
    category: 'STUDY',
    memo: '매주 토요일 2시간씩 기출 위주로 정리하기.',
    status: 'IN_PROGRESS',
    totalCount: 4,
    doneCount: 2,
    items: [
      { id: 11, content: '2024년 기출 3회분 풀이', done: true, doneAt: '2026-02-14T21:10:00', sortOrder: 0 },
      { id: 12, content: 'SQL 파트 요약 정리', done: true, doneAt: '2026-03-02T09:40:00', sortOrder: 1 },
      { id: 13, content: '실무 알고리즘 오답노트', done: false, doneAt: null, sortOrder: 2 },
      { id: 14, content: '모의고사 2회 응시', done: false, doneAt: null, sortOrder: 3 },
    ],
  },
  createDate: '2026-01-04T09:30:00',
  modifyDate: '2026-01-04T09:30:00',
};

/**
 * 데모에서 '보고 있는 사람'으로 칠 회원 id.
 *
 * 목업 프로필의 id 는 'haneul' 같은 문자열이라 서버의 숫자 id 와 맞지 않는다.
 * 본인/타인 분기를 데모에서도 확인할 수 있게 목업 전용으로 둔다.
 */
export const MOCK_GOAL_VIEWER_ID = 1;

/** 남의 성취 — 팀 스페이스 버튼이 숨는지, PR 목록이 비는지 확인용 */
const OTHERS_PROJECT_GOAL: GoalDetailResponse = {
  ...PROJECT_GOAL,
  id: 4,
  ownerId: 2,
  ownerName: '박서준',
  project: { ...PROJECT_GOAL.project!, pullRequests: [] },
};

export const MOCK_GOAL_DETAILS: Record<string, GoalDetailResponse> = {
  '1': PROJECT_GOAL,
  '2': CONTEST_GOAL,
  '3': CHECKLIST_GOAL,
  '4': OTHERS_PROJECT_GOAL,
};

/** 목업에 없는 id 로 들어와도 화면이 비지 않게 PROJECT 성취를 돌려준다 */
export const mockGoalDetail = (goalId: string): GoalDetailResponse =>
  MOCK_GOAL_DETAILS[goalId] ?? { ...PROJECT_GOAL, id: Number(goalId) || PROJECT_GOAL.id };
