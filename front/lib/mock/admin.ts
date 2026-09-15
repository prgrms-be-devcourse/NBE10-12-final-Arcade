import type { AdminStat, HostApproval, ReportItem } from '@/lib/types';

export const MOCK_ADMIN_KPIS: AdminStat[] = [
  { label: '누적 회원', value: '8,412', delta: '지난달 대비 +6.2%' },
  { label: '이번 달 신규', value: '486', delta: '일 평균 16.2명' },
  { label: '주최측 계정', value: '137', delta: '승인 대기 4건' },
  { label: '활성 파티', value: '312', delta: '모집 중 118' },
];

export const MOCK_ADMIN_SIGNUP_CHART = [
  { label: '3월', value: 214, height: 44 },
  { label: '4월', value: 268, height: 55 },
  { label: '5월', value: 301, height: 62 },
  { label: '6월', value: 352, height: 72 },
  { label: '7월', value: 418, height: 86 },
  { label: '8월', value: 486, height: 100 },
];

export const MOCK_ADMIN_SIGNUP_TABLE = [
  { period: '2026.08', general: 462, host: 24, leave: 18, net: '+468' },
  { period: '2026.07', general: 397, host: 21, leave: 22, net: '+396' },
  { period: '2026.06', general: 334, host: 18, leave: 19, net: '+333' },
  { period: '2026.05', general: 287, host: 14, leave: 15, net: '+286' },
];

export const MOCK_ADMIN_PARTIES = [
  { id: 'p1', title: '프로그래머스 오락실 공모전 참여하실분', leader: '정하늘', category: '공모전', applicants: 12, status: '모집중' },
  { id: 'p2', title: '결제 API 안정화 해커톤', leader: '이도경', category: '해커톤', applicants: 8, status: '모집중' },
  { id: 'p3', title: '인디 로그라이크 사이드프로젝트', leader: '박세윤', category: '프로젝트', applicants: 5, status: '모집중' },
  { id: 'p4', title: '프론트엔드 알고리즘 스터디', leader: '최민재', category: '스터디', applicants: 14, status: '마감' },
];

export const MOCK_ADMIN_MEMBERS = [
  { id: 'u1', name: '정하늘', email: 'haneul@crewon.dev', position: '백엔드', joinedAt: '2025.11.02', status: '정상' },
  { id: 'u2', name: '이도경', email: 'dogyeong@crewon.dev', position: '백엔드', joinedAt: '2026.01.15', status: '정상' },
  { id: 'u3', name: '최민재', email: 'minjae@crewon.dev', position: '프론트엔드', joinedAt: '2026.03.08', status: '정상' },
  { id: 'u4', name: '장우진', email: 'woojin@crewon.dev', position: '백엔드', joinedAt: '2026.08.01', status: '정지' },
];

export const MOCK_ADMIN_HOSTS: HostApproval[] = [
  { id: 'h1', company: '코드잼', bizNumber: '123-45-67890', manager: '정하늘', requestedAt: '2025.11.14', status: '승인' },
  { id: 'h2', company: '페이브릿지', bizNumber: '221-88-11234', manager: '박서준', requestedAt: '2026.07.02', status: '승인' },
  { id: 'h3', company: '그린데이터랩', bizNumber: '507-11-99321', manager: '김선우', requestedAt: '2026.08.19', status: '대기' },
  { id: 'h4', company: '서울창업허브', bizNumber: '104-82-00011', manager: '한지우', requestedAt: '2026.08.24', status: '대기' },
];

export const MOCK_ADMIN_BADGES = [
  { id: 'bd1', label: '인기 프로젝트', condition: '전시 조회 1,000회 이상', visible: true },
  { id: 'bd2', label: '스트릭 30일', condition: '30일 연속 활동', visible: true },
  { id: 'bd3', label: '스트릭 100일', condition: '100일 연속 활동', visible: true },
  { id: 'bd4', label: '수상 경험', condition: '공모전 수상 이력 1건 이상', visible: true },
];

export const MOCK_ADMIN_REPORTS: ReportItem[] = [
  { id: 'r1', type: '파티', target: '게임 공모전 도전할 기획자 파티', reporter: '최민재', reason: '허위 모집 정보', createdAt: '2026.08.24', status: '대기' },
  { id: 'r2', type: '전시', target: '팀 매칭 알림 서비스', reporter: '한지우', reason: '타인 결과물 도용 의심', createdAt: '2026.08.22', status: '처리완료' },
  { id: 'r3', type: '쪽지', target: '박서준', reporter: '정하늘', reason: '스팸성 반복 발송', createdAt: '2026.08.20', status: '반려' },
];

export const MOCK_ADMIN_AWARDS = [
  { id: 'aw1', contest: '2025 공공데이터 활용 공모전', team: '그린테크 챌린지 참가팀', rank: '우수상', year: '2025', reflected: true },
  { id: 'aw2', contest: '핀테크 해커톤 시즌 2', team: '페이브릿지 도전팀', rank: '대상', year: '2025', reflected: true },
  { id: 'aw3', contest: 'UX 리디자인 공모전', team: '픽셀 스튜디오', rank: '심사중', year: '2026', reflected: false },
];

export const MOCK_ADMIN_TABS = [
  { key: 'stats', label: '가입 통계' },
  { key: 'party', label: '파티 리스트 관리' },
  { key: 'member', label: '회원 관리' },
  { key: 'host', label: '사업자 가입 승인' },
  { key: 'badge', label: '뱃지 등록 · 관리' },
  { key: 'report', label: '신고 관리' },
  { key: 'award', label: '공모전 수상 이력' },
] as const;
