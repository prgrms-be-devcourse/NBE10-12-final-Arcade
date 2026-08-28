import type { HeroSlide } from '@/lib/types';

export const MOCK_HERO_SLIDES: HeroSlide[] = [
  {
    id: 'slide-1',
    headline: '실력이 곧 {glow}가 되는 팀 매칭',
    headlineGlow: '하이스코어',
    sub: '완료한 프로젝트와 수상 이력이 자동으로 쌓여, 다음 파티의 지원 근거가 됩니다.',
    actions: [
      { label: '파티 둘러보기', href: '/party', variant: 'primary' },
      { label: '성취 프로필 만들기', href: '/mypage', variant: 'ghost' },
    ],
    art: { mark: 'CREW ON', sub: 'INSERT SKILL TO CONTINUE', chips: ['TOP 3', 'LV.14', '1,284 MATCHED'] },
  },
  {
    id: 'slide-2',
    tag: 'NEWS',
    headline: '공모전 허브가 새로 열렸습니다',
    sub: '주최측이 직접 등록한 공모전만 모아, 신뢰할 수 있는 정보로 팀을 구성하세요.',
    actions: [{ label: '공모전 허브 보기', href: '/contests', variant: 'primary' }],
    art: { mark: 'HUB OPEN', sub: '23 CONTESTS LIVE', chips: ['NEW', '실시간 갱신'] },
  },
  {
    id: 'slide-3',
    tag: 'FOR YOU',
    headline: '나와 맞는 파티, 추천으로 먼저 확인',
    sub: '프로필과 성취 키워드를 분석해 잘 맞는 파티를 목록 상단에 올려드립니다.',
    actions: [{ label: '추천 파티 보기', href: '/party', variant: 'primary' }],
    art: { mark: 'MATCH 92', sub: 'KEYWORD FIT SCORE', chips: ['추천', '키워드 매칭'] },
  },
];
