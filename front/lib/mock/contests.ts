import type { Contest, ContestDetail } from '@/lib/types';
import { MOCK_PARTIES } from './parties';

export const MOCK_CONTESTS: Contest[] = [
  {
    id: 'public-data',
    title: '2026 공공데이터 활용 챌린지',
    host: '한빛데이터진흥원',
    hostId: 'hanbit',
    format: 'COMPETITION',
    tag: '데이터',
    status: '접수중',
    prize: '상금 500만원',
    dday: 'D-12',
    period: '2026.08.01 ~ 09.10',
    linkUrl: 'https://example.com/contests/public-data',
    viewCount: 3120,
    likeCount: 214,
    teams: 18,
  },
  {
    id: 'greentech',
    title: '그린테크 아이디어 챌린지',
    host: '그린데이터랩',
    hostId: 'greendatalab',
    format: 'COMPETITION',
    tag: '환경',
    status: '접수중',
    prize: '상금 300만원',
    dday: 'D-20',
    period: '2026.08.10 ~ 09.18',
    linkUrl: 'https://example.com/contests/greentech',
    coverImageUrl: '/samples/cover-green.svg',
    viewCount: 1845,
    likeCount: 96,
    teams: 9,
  },
  {
    id: 'fintech-hackathon',
    title: '핀테크 해커톤 시즌 3',
    host: '페이브릿지',
    hostId: 'paybridge',
    format: 'HACKATHON',
    tag: '핀테크',
    status: '마감임박',
    prize: '상금 400만원',
    dday: 'D-6',
    period: '2026.07.20 ~ 08.26',
    linkUrl: 'https://example.com/contests/fintech-hackathon',
    viewCount: 4207,
    likeCount: 302,
    teams: 24,
  },
  {
    id: 'ux-redesign',
    title: 'UX 리디자인 공모전',
    host: '서울창업허브',
    hostId: 'seoulhub',
    format: 'COMPETITION',
    tag: 'UX',
    status: '접수중',
    prize: '상금 200만원',
    dday: 'D-30',
    period: '2026.08.01 ~ 09.28',
    linkUrl: 'https://example.com/contests/ux-redesign',
    viewCount: 1036,
    likeCount: 58,
    teams: 12,
  },
  {
    id: 'ai-labeling',
    title: 'AI 데이터 라벨링 챌린지',
    host: '한빛데이터진흥원',
    hostId: 'hanbit',
    format: 'HACKATHON',
    tag: 'AI',
    status: '접수중',
    prize: '상금 350만원',
    dday: 'D-16',
    period: '2026.08.05 ~ 09.14',
    linkUrl: 'https://example.com/contests/ai-labeling',
    viewCount: 2588,
    likeCount: 173,
    teams: 15,
  },
  {
    id: 'local-economy',
    title: '지역상권 활성화 아이디어 공모전',
    host: '서울창업허브',
    hostId: 'seoulhub',
    format: 'COMPETITION',
    tag: '지역경제',
    status: '접수중',
    prize: '상금 250만원',
    dday: 'D-24',
    period: '2026.08.02 ~ 09.22',
    linkUrl: 'https://example.com/contests/local-economy',
    viewCount: 742,
    likeCount: 31,
    teams: 7,
  },
  {
    id: 'arcade',
    title: '프로그래머스 오락실 공모전',
    host: '코드잼 운영팀',
    hostId: 'codejam',
    format: 'COMPETITION',
    tag: '기타',
    status: '접수중',
    prize: '상금 300만원',
    dday: 'D-9',
    period: '2026.08.05 ~ 09.04',
    linkUrl: 'https://example.com/contests/arcade',
    coverImageUrl: '/samples/cover-arcade.svg',
    viewCount: 3120,
    likeCount: 188,
    teams: 18,
  },
  {
    id: 'campus-hack',
    title: '캠퍼스 오픈소스 해커톤',
    host: '대학연합 오픈소스 동아리',
    hostId: 'univ-oss',
    format: 'HACKATHON',
    tag: '기타',
    status: '접수중',
    prize: '상금 150만원',
    dday: 'D-27',
    period: '2026.08.12 ~ 09.25',
    linkUrl: 'https://example.com/contests/campus-hack',
    viewCount: 913,
    likeCount: 44,
    teams: 11,
  },
];

/** 대회별 소개 — 목록 데이터를 그대로 확장한다 (시상 내역·일정은 소개 글에 적는다) */
const DESCRIPTIONS: Record<string, { description: string; target: string }> = {
  'public-data': {
    description:
      '공공데이터를 활용한 서비스 아이디어를 발굴하는 챌린지입니다. 예선 통과 팀은 실제 공공 API 연동 지원과 멘토링을 받을 수 있어요.',
    target: '대학생 · 일반인 누구나 (2~5인 팀)',
  },
  greentech: {
    description:
      '탄소 감축과 친환경 소비를 주제로 한 아이디어 챌린지입니다. 데이터 기반으로 실제 행동 변화를 이끌어내는 서비스를 찾습니다.',
    target: '제한 없음 (2~4인 팀)',
  },
  'fintech-hackathon': {
    description:
      '48시간 동안 결제·정산 도메인의 문제를 푸는 해커톤입니다. 페이브릿지의 샌드박스 결제 API가 참가팀 전원에게 제공됩니다.',
    target: '현직 개발자 · 취업준비생 (3~5인 팀)',
  },
  'ux-redesign': {
    description:
      '공공 서비스 웹·앱을 사용자 관점에서 다시 설계하는 공모전입니다. 리서치 과정과 개선 근거를 함께 제출해야 합니다.',
    target: '디자인 전공자 · 실무자 (개인 또는 2~3인 팀)',
  },
  'ai-labeling': {
    description:
      '라벨링 품질을 자동으로 검증하는 파이프라인을 만드는 챌린지입니다. 실제 라벨링 데이터셋 일부가 참가팀에 공개됩니다.',
    target: '제한 없음 (2~4인 팀)',
  },
  'local-economy': {
    description:
      '골목상권 데이터를 활용해 지역 상권을 살리는 아이디어를 찾습니다. 수상팀은 자치구 시범사업 연계를 검토합니다.',
    target: '서울 소재 대학생 · 소상공인 (2~5인 팀)',
  },
  arcade: {
    description:
      '레트로 오락실을 주제로 한 웹 미니게임 공모전입니다. 게임성과 함께 랭킹·매칭 같은 서버 기능 구현도 심사에 반영됩니다.',
    target: '제한 없음 (2~5인 팀)',
  },
  'campus-hack': {
    description:
      '기존 오픈소스 프로젝트에 기여하거나 새 프로젝트를 시작하는 무박 2일 해커톤입니다. 커밋 이력이 심사 자료로 활용됩니다.',
    target: '재학생 · 졸업 2년 이내 (3~5인 팀)',
  },
};

export const MOCK_CONTEST_DETAILS: Record<string, ContestDetail> = Object.fromEntries(
  MOCK_CONTESTS.map((contest) => [
    contest.id,
    {
      ...contest,
      ...DESCRIPTIONS[contest.id],
      relatedParties: MOCK_PARTIES.filter((party) => party.contestId === contest.id),
    },
  ]),
) as Record<string, ContestDetail>;
