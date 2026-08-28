import type { HostCompany, HostContestSummary } from '@/lib/types';
import { MOCK_CONTESTS } from './contests';

export const MOCK_HOST_COMPANY: HostCompany = {
  id: 'codejam',
  name: '코드잼',
  bizNumber: '123-45-*****',
  verified: true,
  manager: '정하늘 · 인재영입팀',
  email: 'haneul@codejam.co.kr',
  phone: '02-000-0000',
  homepage: 'codejam.co.kr',
  intro:
    '게임·웹 분야 공모전을 운영합니다. 크루온에서 팀 단위 참가 접수와 진행 현황을 관리하고 있어요.',
};

export const MOCK_HOST_STATS = [
  { label: '등록 공모전', value: '4' },
  { label: '접수 중', value: '2' },
  { label: '누적 참가팀', value: '63' },
  { label: '누적 조회', value: '12,480' },
];

export const MOCK_HOST_CONTESTS: HostContestSummary[] = [
  { ...MOCK_CONTESTS[6], status: '접수중', dday: 'D-9', linkedParties: 6, submissions: 0 },
  { ...MOCK_CONTESTS[2], status: '접수중', dday: 'D-6', linkedParties: 11, submissions: 0 },
  { ...MOCK_CONTESTS[3], status: '마감', dday: '심사 중', linkedParties: 12, submissions: 12 },
  {
    ...MOCK_CONTESTS[0],
    id: 'public-data-2025',
    title: '2025 공공데이터 활용 공모전',
    status: '마감',
    dday: '종료',
    period: '2025.09 종료',
    viewCount: 4117,
    likeCount: 260,
    teams: 9,
    linkedParties: 9,
    submissions: 9,
  },
];

export const MOCK_HOST_TEAMS = [
  { name: '페이브릿지 해커톤 도전팀', contest: '핀테크 해커톤 시즌 3', members: 2, progress: '진행 8/12', live: true },
  { name: '오락실 픽셀 던전팀', contest: '프로그래머스 오락실 공모전', members: 4, progress: '진행 3/10', live: true },
  { name: '그린테크 챌린지 참가팀', contest: '2025 공공데이터 활용 공모전', members: 3, progress: '제출 완료', live: false },
];

export const MOCK_HOST_FILES = [
  { id: 'f1', name: '사업자등록증.pdf', size: '412KB', uploadedAt: '2025.11.14' },
  { id: 'f2', name: '통신판매업신고증.pdf', size: '288KB', uploadedAt: '2025.11.14' },
];
