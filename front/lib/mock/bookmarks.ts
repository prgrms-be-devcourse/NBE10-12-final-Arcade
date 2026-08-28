import type { BookmarkItem } from '@/lib/types';

/**
 * 북마크함 — 파티·대회·성취를 대상 구분 없이 한 목록으로 보여준다 (기획서 2.11).
 * 실제 API 에서는 targetType + targetId 로 저장된 BOOKMARK 행을 조인해 내려준다 (기획서 3.2).
 */
export const MOCK_BOOKMARKS: BookmarkItem[] = [
  {
    id: 'bm1',
    targetType: 'PARTY',
    targetId: 'payment',
    title: '결제 API 안정화 해커톤, 백엔드 한 명 더 구해요',
    subtitle: '이도경 · 백엔드 1/2 · 지원자 8명',
    meta: 'D-6',
    tags: ['대회', '해커톤'],
    createdAt: '2026.08.21',
  },
  {
    id: 'bm2',
    targetType: 'GOAL',
    targetId: 'payment-retry',
    title: '결제 재시도 큐',
    subtitle: '결제 API 안정화 해커톤팀 · 백엔드',
    meta: '조회 1,247',
    tags: ['플랫폼 자동기록', 'Kafka'],
    createdAt: '2026.08.19',
  },
  {
    id: 'bm3',
    targetType: 'CONTEST',
    targetId: 'fintech-hackathon',
    title: '핀테크 해커톤 시즌 3',
    subtitle: '페이브릿지 · 접수 2026.07.20 ~ 08.26',
    meta: 'D-6',
    tags: ['해커톤', '핀테크'],
    createdAt: '2026.08.15',
  },
  {
    id: 'bm4',
    targetType: 'CONTEST',
    targetId: 'ai-labeling',
    title: 'AI 데이터 라벨링 챌린지',
    subtitle: '한빛데이터진흥원 · 상금 350만원',
    meta: 'D-16',
    tags: ['해커톤', 'AI'],
    createdAt: '2026.08.12',
  },
  {
    id: 'bm5',
    targetType: 'PARTY',
    targetId: 'algostudy',
    title: '프론트엔드 알고리즘 스터디원 모집 (주 2회)',
    subtitle: '최민재 · 스터디원 3/5 · 지원자 14명',
    meta: 'D-9',
    tags: ['스터디', '웹 개발'],
    createdAt: '2026.08.09',
  },
  {
    id: 'bm6',
    targetType: 'GOAL',
    targetId: 'pixel-dungeon',
    title: '픽셀 던전 크롤러',
    subtitle: '인디 로그라이크 사이드프로젝트 · 클라이언트',
    meta: '조회 842',
    tags: ['자기신고', 'Unity'],
    createdAt: '2026.08.03',
  },
];
