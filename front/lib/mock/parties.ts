import type { Party, PartyDetail } from '@/lib/types';
import { MOCK_USER_SUMMARIES } from './users';

export const MOCK_PARTIES: Party[] = [
  {
    id: 'oakroom',
    title: '프로그래머스 오락실 공모전 참여하실분',
    summary: '오락실 컨셉의 미니게임 웹서비스를 3주 안에 완성하는 것이 목표예요.',
    topicType: 'CONTEST',
    contestFormat: 'COMPETITION',
    contestId: 'arcade',
    contestName: '프로그래머스 오락실 공모전',
    contestLinkUrl: 'https://example.com/contests/arcade',
    subCategory: '웹 개발',
    positions: [
      { type: 'FRONT', capacity: 2, filledCount: 2 },
      { type: 'BACK', capacity: 2, filledCount: 1 },
    ],
    applicants: 12,
    dday: 'D-3',
    deadline: '2026.09.04',
    createdAt: '2026.08.05',
    leader: MOCK_USER_SUMMARIES.haneul,
    tags: ['백엔드', '랭킹 API'],
    matchScore: 92,
    likeCount: 47,
    viewCount: 340,
    status: 'RECRUITING',
  },
  {
    id: 'payment',
    title: '결제 API 안정화 해커톤, 백엔드 한 명 더 구해요',
    summary: '결제 재시도 큐를 안정화하는 해커톤 팀입니다.',
    topicType: 'CONTEST',
    contestFormat: 'HACKATHON',
    contestId: 'fintech-hackathon',
    contestName: '핀테크 해커톤 시즌 3',
    contestLinkUrl: 'https://example.com/contests/fintech-hackathon',
    subCategory: '앱 개발',
    positions: [
      { type: 'BACK', capacity: 3, filledCount: 2 },
      { type: 'FRONT', capacity: 1, filledCount: 0 },
    ],
    applicants: 8,
    dday: 'D-6',
    deadline: '2026.09.07',
    createdAt: '2026.08.10',
    leader: MOCK_USER_SUMMARIES.dogyeong,
    tags: ['백엔드', '결제 시스템'],
    matchScore: 78,
    likeCount: 29,
    viewCount: 212,
    bookmarkedByMe: true,
    status: 'RECRUITING',
  },
  {
    id: 'roguelike',
    title: '인디 로그라이크 사이드프로젝트 클라이언트 구합니다',
    summary: 'Unity 기반 로그라이크 게임을 함께 만들 클라이언트를 찾아요.',
    topicType: 'PROJECT',
    subCategory: '게임 개발',
    positions: [
      { type: 'FRONT', capacity: 3, filledCount: 2 },
      { type: 'BACK', capacity: 1, filledCount: 0 },
    ],
    applicants: 5,
    dday: 'D-12',
    deadline: '2026.09.13',
    createdAt: '2026.08.02',
    leader: MOCK_USER_SUMMARIES.seyoon,
    tags: ['Unity', 'C#'],
    likeCount: 18,
    viewCount: 164,
    status: 'RECRUITING',
  },
  {
    id: 'algostudy',
    title: '프론트엔드 알고리즘 스터디원 모집 (주 2회)',
    summary: '주 2회 온라인으로 진행하는 알고리즘 스터디입니다.',
    topicType: 'STUDY',
    subCategory: '웹 개발',
    positions: [{ type: 'FRONT', capacity: 5, filledCount: 3 }],
    applicants: 14,
    dday: 'D-9',
    deadline: '2026.09.10',
    createdAt: '2026.08.08',
    leader: MOCK_USER_SUMMARIES.minjae,
    tags: ['알고리즘', '스터디'],
    likeCount: 34,
    viewCount: 287,
    status: 'RECRUITING',
  },
  {
    id: 'reservation',
    title: '소상공인 예약 서비스 사이드프로젝트 같이 하실 분',
    summary: '소상공인을 위한 간편 예약 서비스를 만들고 있어요.',
    topicType: 'ETC',
    subCategory: '앱 개발',
    positions: [
      { type: 'FRONT', capacity: 1, filledCount: 1 },
      { type: 'BACK', capacity: 1, filledCount: 1 },
    ],
    applicants: 3,
    dday: 'D-20',
    deadline: '2026.09.21',
    createdAt: '2026.07.30',
    leader: MOCK_USER_SUMMARIES.jiwoo,
    tags: ['Node.js', 'PostgreSQL'],
    likeCount: 11,
    viewCount: 98,
    status: 'RECRUITING',
  },
  {
    id: 'gamecontest',
    title: '게임 공모전 도전할 기획자 파티 구합니다',
    summary: '게임 공모전 출품작을 함께 만들 클라이언트를 모집해요.',
    topicType: 'CONTEST',
    contestFormat: 'COMPETITION',
    contestName: '전국 인디게임 공모전',
    contestLinkUrl: 'https://example.com/indie-game-contest',
    subCategory: '게임 개발',
    positions: [
      { type: 'BACK', capacity: 1, filledCount: 1 },
      { type: 'FRONT', capacity: 3, filledCount: 1 },
    ],
    applicants: 21,
    dday: 'D-1',
    deadline: '2026.08.27',
    createdAt: '2026.07.25',
    leader: MOCK_USER_SUMMARIES.sehoon,
    tags: ['게임 기획'],
    likeCount: 52,
    viewCount: 431,
    status: 'RECRUITING',
  },
  {
    id: 'sidefe',
    title: '사이드프로젝트 프론트·UI/UX 구합니다',
    summary: '사이드프로젝트에서 프론트엔드와 UI/UX를 맡아줄 분을 찾아요.',
    topicType: 'PROJECT',
    subCategory: '웹 개발',
    positions: [
      { type: 'FRONT', capacity: 2, filledCount: 0 },
    ],
    applicants: 3,
    dday: 'D-15',
    deadline: '2026.09.16',
    createdAt: '2026.08.11',
    leader: MOCK_USER_SUMMARIES.haneul,
    tags: ['React', 'Figma'],
    likeCount: 9,
    viewCount: 76,
    status: 'RECRUITING',
  },
  {
    id: 'paybridge',
    title: '페이브릿지 해커톤 도전팀',
    summary: '정산 자동화 API로 핀테크 해커톤에 도전하는 팀입니다.',
    topicType: 'CONTEST',
    contestFormat: 'HACKATHON',
    contestId: 'fintech-hackathon',
    contestName: '핀테크 해커톤 시즌 3',
    contestLinkUrl: 'https://example.com/contests/fintech-hackathon',
    subCategory: '웹 개발',
    positions: [
      { type: 'BACK', capacity: 1, filledCount: 1 },
      { type: 'FRONT', capacity: 1, filledCount: 1 },
    ],
    applicants: 47,
    dday: 'D-3',
    deadline: '2026.08.26',
    createdAt: '2026.07.28',
    leader: MOCK_USER_SUMMARIES.haneul,
    tags: ['Spring Boot', 'Redis'],
    likeCount: 128,
    viewCount: 2064,
    status: 'IN_PROGRESS',
  },
  {
    id: 'greentech-party',
    title: '그린테크 챌린지 참가팀',
    summary: '친환경 소비 리포트를 만들어 그린테크 챌린지에 참가했습니다.',
    topicType: 'CONTEST',
    contestFormat: 'COMPETITION',
    contestId: 'greentech',
    contestName: '그린테크 아이디어 챌린지',
    contestLinkUrl: 'https://example.com/contests/greentech',
    subCategory: '웹 개발',
    positions: [
      { type: 'BACK', capacity: 2, filledCount: 2 },
    ],
    applicants: 21,
    dday: '완료',
    deadline: '2025.11.18',
    createdAt: '2025.10.02',
    leader: MOCK_USER_SUMMARIES.haneul,
    tags: ['PostgreSQL', 'FTS'],
    likeCount: 94,
    viewCount: 1512,
    status: 'COMPLETED',
  },
  {
    id: 'commerce-clone',
    title: '커머스 클론 사이드프로젝트',
    summary: '커머스 서비스를 클론하며 대용량 트래픽 설계를 연습합니다.',
    topicType: 'PROJECT',
    subCategory: '웹 개발',
    positions: [
      { type: 'FRONT', capacity: 1, filledCount: 0 },
      { type: 'BACK', capacity: 2, filledCount: 1 },
    ],
    applicants: 29,
    dday: 'D-9',
    deadline: '2026.09.10',
    createdAt: '2026.08.06',
    leader: MOCK_USER_SUMMARIES.dogyeong,
    tags: ['커머스', 'Redis'],
    likeCount: 63,
    viewCount: 522,
    status: 'RECRUITING',
  },
];

/** 홈 TOP3 — 좋아요 수(likeCount) 기준 상위 3건 (기획서 2.1) */
export const MOCK_TOP_PARTIES: Party[] = [...MOCK_PARTIES]
  .sort((a, b) => b.likeCount - a.likeCount)
  .slice(0, 3);

const REQUIREMENTS: Record<string, Partial<Record<string, string>>> = {
  oakroom: {
    FRONT: 'React 기반 픽셀 UI 구현 경험',
    BACK: 'Spring Boot, JPA로 매칭 · 랭킹 API 설계',
    UIUX: '레트로 아케이드 컨셉 디자인 시스템',
  },
  payment: {
    BACK: '결제 재시도 큐 · 멱등성 설계 경험',
    FRONT: '대시보드 화면 구현',
    PM: '해커톤 일정 관리와 발표 준비',
  },
};

/** 목록의 파티를 그대로 상세로 확장 — 모든 id 로 상세 진입이 가능하다 */
export const MOCK_PARTY_DETAILS: Record<string, PartyDetail> = Object.fromEntries(
  MOCK_PARTIES.map((party) => [
    party.id,
    {
      ...party,
      description: `${party.summary} 주 2회 온라인 스크럼으로 진행하고, 완주하면 참여 이력이 자동으로 성취 프로필에 남습니다.`,
      schedule: '주 2회 온라인 스크럼',
      meetingType: '온라인',
      members: party.positions
        .filter((position) => position.filledCount > 0)
        .map((position, index) => ({
          ...(index === 0
            ? party.leader
            : Object.values(MOCK_USER_SUMMARIES)[(index * 3) % 12]),
        })),
      requirements: party.positions.map(
        (position) =>
          REQUIREMENTS[party.id]?.[position.type] ??
          '자세한 조건은 파티 소개를 확인해 주세요.',
      ),
    },
  ]),
) as Record<string, PartyDetail>;

/** 추천 파티(FOR YOU) 섹션 문구 */
export const MOCK_RECOMMEND_KEYWORDS = '백엔드 · Spring Boot · PostgreSQL';

export const MOCK_RECOMMENDED_PARTIES: (Party & { why: string })[] = [
  { ...MOCK_PARTIES[0], why: '"백엔드 · 랭킹 API" 키워드가 일치해요' },
  { ...MOCK_PARTIES[1], why: '"백엔드 · 결제 시스템" 키워드가 일치해요' },
];
