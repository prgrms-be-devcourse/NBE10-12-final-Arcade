import type { ExhibitionDetail, ExhibitionProject } from '@/lib/types';
import { MOCK_USER_SUMMARIES } from './users';

export const MOCK_EXHIBITIONS: ExhibitionProject[] = [
  {
    id: 'settlement-api',
    title: '정산 자동화 API',
    summary: '페이브릿지 해커톤 도전팀 · 백엔드',
    partyName: '페이브릿지 해커톤 도전팀',
    role: 'BACK',
    category: '웹 개발',
    source: 'PLATFORM_VERIFIED',
    sourcePartyId: 'paybridge',
    skills: ['Spring Boot', 'Redis'],
    viewCount: 2064,
    likeCount: 128,
    coverImageUrl: '/samples/cover-settlement.svg',
    leader: MOCK_USER_SUMMARIES.haneul,
    thumbnailLabel: '정산',
  },
  {
    id: 'green-report',
    title: '친환경 소비 리포트',
    summary: '그린테크 챌린지 참가팀 · 백엔드',
    partyName: '그린테크 챌린지 참가팀',
    role: 'BACK',
    category: '웹 개발',
    source: 'PLATFORM_VERIFIED',
    sourcePartyId: 'greentech-party',
    skills: ['PostgreSQL', 'FTS'],
    viewCount: 1512,
    likeCount: 94,
    coverImageUrl: '/samples/cover-green.svg',
    leader: MOCK_USER_SUMMARIES.haneul,
    thumbnailLabel: '그린',
  },
  {
    id: 'payment-retry',
    title: '결제 재시도 큐',
    summary: '결제 API 안정화 해커톤팀 · 백엔드',
    partyName: '결제 API 안정화 해커톤팀',
    role: 'BACK',
    category: '앱 개발',
    source: 'PLATFORM_VERIFIED',
    sourcePartyId: 'payment',
    skills: ['Spring Boot', 'Kafka'],
    viewCount: 1247,
    likeCount: 61,
    leader: MOCK_USER_SUMMARIES.dogyeong,
    thumbnailLabel: '결제',
  },
  {
    id: 'pixel-dungeon',
    title: '픽셀 던전 크롤러',
    summary: '인디 로그라이크 사이드프로젝트 · 클라이언트',
    partyName: '인디 로그라이크 사이드프로젝트',
    role: 'FRONT',
    category: '게임 개발',
    source: 'SELF_REPORTED',
    skills: ['Unity', 'C#'],
    viewCount: 842,
    likeCount: 77,
    leader: MOCK_USER_SUMMARIES.seyoon,
    thumbnailLabel: '던전',
  },
  {
    id: 'reservation-system',
    title: '소상공인 예약 시스템',
    summary: '소상공인 예약 서비스 사이드프로젝트 · 풀스택',
    partyName: '소상공인 예약 서비스 사이드프로젝트',
    role: 'BACK',
    category: '앱 개발',
    source: 'PLATFORM_VERIFIED',
    sourcePartyId: 'reservation',
    skills: ['Node.js', 'PostgreSQL'],
    viewCount: 571,
    likeCount: 52,
    leader: MOCK_USER_SUMMARIES.jiwoo,
    thumbnailLabel: '예약',
  },
  {
    id: 'match-notifier',
    title: '팀 매칭 알림 서비스',
    summary: '개인 토이프로젝트 · 백엔드',
    partyName: '개인 토이프로젝트',
    role: 'BACK',
    category: '기타',
    source: 'SELF_REPORTED',
    skills: ['WebSocket'],
    viewCount: 986,
    likeCount: 35,
    leader: MOCK_USER_SUMMARIES.haneul,
    thumbnailLabel: '알림',
  },
];

export const MOCK_EXHIBITION_CATEGORIES = ['전체', '웹 개발', '게임 개발', '앱 개발', '기타'];

/**
 * 완료 시점 스냅샷 — 팀 스페이스의 커밋 내역을 그대로 담는다.
 * (체크리스트가 커밋 기반으로 바뀌면서 전시 상세의 '진행 내역'도 커밋으로 통일)
 */
export const MOCK_EXHIBITION_COMMITS: Record<
  string,
  { sha: string; message: string; authorName: string; authorInitial: string; date: string; approvers: string[] }[]
> = {
  'settlement-api': [
    { sha: 'bb90e63', message: '결제 재시도 큐 프로토타입 구현', authorName: '정하늘', authorInitial: '정', date: '2026.08.20', approvers: ['윤소민'] },
    { sha: 'e05b7f1', message: 'Redis 캐시 TTL 설정값 환경변수로 분리', authorName: '정하늘', authorInitial: '정', date: '2026.08.22', approvers: ['윤소민'] },
    { sha: '30cd9ab', message: '차트 컴포넌트 리팩터링 및 로딩 상태 추가', authorName: '윤소민', authorInitial: '윤', date: '2026.08.23', approvers: ['정하늘'] },
    { sha: '9f3a1c7', message: '정산 배치 실패 시 슬랙 알림 전송 추가', authorName: '정하늘', authorInitial: '정', date: '2026.08.24', approvers: ['윤소민'] },
  ],
  'green-report': [
    { sha: '5c21a90', message: '소비 데이터 수집 배치 구현', authorName: '정하늘', authorInitial: '정', date: '2025.10.21', approvers: ['오세훈'] },
    { sha: 'f80b3ce', message: '전문 검색(FTS) 인덱스 설계', authorName: '정하늘', authorInitial: '정', date: '2025.11.02', approvers: ['오세훈'] },
    { sha: '17ad442', message: '리포트 화면 기획 반영', authorName: '오세훈', authorInitial: '오', date: '2025.11.09', approvers: ['정하늘'] },
  ],
  'payment-retry': [
    { sha: 'c93de17', message: 'Kafka 재시도 토픽 설계', authorName: '이도경', authorInitial: '이', date: '2025.06.11', approvers: ['한지우'] },
    { sha: '2a4f6bb', message: '멱등키 기반 중복 결제 차단', authorName: '이도경', authorInitial: '이', date: '2025.06.18', approvers: ['한지우'] },
  ],
  'reservation-system': [
    { sha: '81ce0d5', message: '예약 충돌 검증 로직 구현', authorName: '한지우', authorInitial: '한', date: '2026.07.14', approvers: ['최민재'] },
    { sha: 'd6702af', message: '사장님용 관리 화면 구현', authorName: '최민재', authorInitial: '최', date: '2026.07.22', approvers: ['한지우'] },
  ],
};

const DESCRIPTIONS: Record<string, string> = {
  'settlement-api':
    '페이브릿지 해커톤 도전팀에서 만든 정산 자동화 API입니다. 매칭 로직과 랭킹 API, 정산 배치, Redis 캐시 레이어를 담당했어요.',
  'green-report':
    '카드 소비 내역에서 탄소 배출량을 추정해 주간 리포트로 보여주는 서비스입니다. 데이터 수집 배치와 PostgreSQL 전문 검색을 맡았습니다.',
  'payment-retry':
    '결제 실패 건을 Kafka 큐에 넣어 지수 백오프로 재시도하는 모듈입니다. 멱등키 설계로 중복 결제를 차단했어요.',
  'pixel-dungeon':
    'Unity로 만든 로그라이크 던전 크롤러입니다. 절차적 맵 생성과 턴제 전투 시스템을 직접 구현했습니다.',
  'reservation-system':
    '전화로만 예약을 받던 동네 가게를 위한 예약 서비스입니다. 예약 충돌 검증과 사장님용 관리 화면을 만들었어요.',
  'match-notifier':
    'WebSocket으로 새 파티 모집글을 실시간 알림으로 받아보는 개인 토이프로젝트입니다.',
};

const PERIODS: Record<string, string> = {
  'settlement-api': '2026.08.03 ~ 2026.08.24',
  'green-report': '2025.10.02 ~ 2025.11.18',
  'payment-retry': '2025.05.20 ~ 2025.06.28',
  'pixel-dungeon': '2026.03.01 ~ 2026.06.30',
  'reservation-system': '2026.06.02 ~ 2026.07.28',
  'match-notifier': '2024.02.10 ~ 2024.03.30',
};

const COMMENTS: Record<
  string,
  { author: string; content: string; createdAt: string; replies?: { author: string; content: string; createdAt: string }[] }[]
> = {
  'settlement-api': [
    {
      author: 'dogyeong',
      content: '정산 배치 설계가 인상적이네요. 재시도 정책은 어떻게 잡으셨나요?',
      createdAt: '2일 전',
      replies: [
        {
          author: 'haneul',
          content: '지수 백오프로 최대 3회까지만 재시도하고, 그 뒤엔 실패 큐로 보냅니다.',
          createdAt: '2일 전',
        },
      ],
    },
    { author: 'minjae', content: '픽셀 UI랑 잘 어울려요! 다음 파티도 기대됩니다.', createdAt: '어제' },
  ],
  'green-report': [
    {
      author: 'jiwoo',
      content: '탄소 배출량 추정 기준이 궁금해요. 공개된 계수를 쓰신 건가요?',
      createdAt: '5일 전',
    },
  ],
  'pixel-dungeon': [
    { author: 'sehoon', content: '맵 생성 알고리즘 글로도 정리해 주시면 좋겠어요.', createdAt: '3일 전' },
    { author: 'doyoon', content: '플레이해봤는데 타격감이 좋네요 👍', createdAt: '2일 전' },
  ],
  'match-notifier': [{ author: 'haram', content: '알림 지연은 어느 정도인가요?', createdAt: '1주 전' }],
};

export const MOCK_EXHIBITION_DETAILS: Record<string, ExhibitionDetail> = Object.fromEntries(
  MOCK_EXHIBITIONS.map((project) => [
    project.id,
    {
      ...project,
      description: DESCRIPTIONS[project.id],
      period: PERIODS[project.id],
      members:
        project.source === 'PLATFORM_VERIFIED'
          ? [project.leader, MOCK_USER_SUMMARIES.somin]
          : [project.leader],
      links: [
        {
          id: 'gh',
          label: 'GitHub',
          url: `https://github.com/crewon/${project.id}`,
        },
      ],
      comments: (COMMENTS[project.id] ?? []).map((comment, index) => ({
        id: `${project.id}-cm${index + 1}`,
        authorName: MOCK_USER_SUMMARIES[comment.author].name,
        authorInitial: MOCK_USER_SUMMARIES[comment.author].initial,
        content: comment.content,
        createdAt: comment.createdAt,
        replies: (comment.replies ?? []).map((reply, replyIndex) => ({
          id: `${project.id}-cm${index + 1}-r${replyIndex + 1}`,
          authorName: MOCK_USER_SUMMARIES[reply.author].name,
          authorInitial: MOCK_USER_SUMMARIES[reply.author].initial,
          content: reply.content,
          createdAt: reply.createdAt,
          replies: [],
        })),
      })),
    },
  ]),
) as Record<string, ExhibitionDetail>;
