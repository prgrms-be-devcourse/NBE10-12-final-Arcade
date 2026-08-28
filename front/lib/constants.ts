import type {
  ContestFormat,
  ContestTag,
  GoalStatus,
  GoalType,
  PartyStatus,
  PositionType,
  TopicType,
} from './types';

/**
 * 도메인 enum 의 화면 표기 문구와 선택지 목록.
 *
 * enum 값(영문)은 서버와 주고받는 값이고, 화면에는 항상 여기 라벨을 쓴다.
 * 라벨이 바뀌어도 API 계약은 그대로 유지된다.
 */

/* ---------- 포지션 (기획서 2.1) ---------- */

export const POSITION_TYPES: readonly PositionType[] = ['BACK', 'FRONT'];

export const POSITION_LABELS: Record<PositionType, string> = {
  BACK: '백엔드',
  FRONT: '프론트엔드',
};

export const positionLabel = (type: PositionType) => POSITION_LABELS[type];

/* ---------- 파티 주제 유형 (기획서 3.5) ---------- */

export const TOPIC_TYPES: readonly TopicType[] = ['CONTEST', 'PROJECT', 'STUDY', 'ETC'];

export const TOPIC_TYPE_LABELS: Record<TopicType, string> = {
  CONTEST: '대회',
  PROJECT: '프로젝트',
  STUDY: '스터디',
  ETC: '기타',
};

/* ---------- 대회 형식 · 분야 (기획서 2.4, 3.5) ---------- */

export const CONTEST_FORMATS: readonly ContestFormat[] = ['COMPETITION', 'HACKATHON'];

export const CONTEST_FORMAT_LABELS: Record<ContestFormat, string> = {
  COMPETITION: '공모전',
  HACKATHON: '해커톤',
};

export const CONTEST_TAGS: readonly ContestTag[] = [
  '데이터',
  '환경',
  '핀테크',
  'UX',
  'AI',
  '지역경제',
  '기타',
];

/* ---------- 파티 상태 (기획서 2.1) ---------- */

export const PARTY_STATUS_LABELS: Record<PartyStatus, string> = {
  RECRUITING: '모집중',
  IN_PROGRESS: '진행중',
  COMPLETED: '완료',
};

/* ---------- 성취 (기획서 3.6) ---------- */

export const GOAL_TYPES: readonly GoalType[] = ['PROJECT', 'CONTEST', 'CHECKLIST'];

export const GOAL_TYPE_LABELS: Record<GoalType, string> = {
  PROJECT: '프로젝트',
  CONTEST: '수상·대회',
  CHECKLIST: '체크리스트',
};

export const GOAL_STATUSES: readonly GoalStatus[] = ['WANT', 'IN_PROGRESS', 'HOLD', 'ACHIEVED'];

export const GOAL_STATUS_LABELS: Record<GoalStatus, string> = {
  WANT: '하고싶음',
  IN_PROGRESS: '진행중',
  HOLD: '보류',
  ACHIEVED: '달성',
};

/** 상태 전이 규칙 — ACHIEVED 는 종료 상태 (기획서 3.6) */
export const GOAL_STATUS_TRANSITIONS: Record<GoalStatus, readonly GoalStatus[]> = {
  WANT: ['IN_PROGRESS', 'ACHIEVED'],
  IN_PROGRESS: ['HOLD', 'ACHIEVED'],
  HOLD: ['IN_PROGRESS', 'ACHIEVED'],
  ACHIEVED: [],
};

export const GOAL_SOURCE_LABELS = {
  PLATFORM_VERIFIED: '플랫폼 자동기록',
  SELF_REPORTED: '자기신고',
} as const;

/* ---------- 파티 분야 ---------- */

/**
 * 기획서 3.5 는 PARTY 에 분야 필드를 두지 않지만,
 * 목업의 게시판 필터를 유지하기 위해 잠정적으로 남겨둔 값이다. (팀 확인 후 정리 예정)
 */
export const PARTY_FIELDS = ['웹 개발', '게임 개발', '앱 개발', '기타'] as const;
