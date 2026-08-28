/**
 * 크루온(CrewOn) 도메인 타입.
 *
 * 실제 백엔드 API 응답 스키마가 확정되면 이 파일만 수정하면 되도록,
 * 화면 컴포넌트는 전부 이 타입에만 의존한다.
 */

/* ---------- 공통 ---------- */

export type ID = string;

/* ---------- 도메인 공통 enum (기획서 2.1 / 2.4 / 3.5 / 3.6) ---------- */

/**
 * 포지션 — 고정 enum. 자유 입력 문자열을 쓰지 않는다 (기획서 2.1).
 * UIUX·PM 은 이번 스코프에서 쓰지 않기로 해 제외했다.
 */
export type PositionType = 'BACK' | 'FRONT';

/** 파티 주제 유형 — 공모전·해커톤은 '대회(CONTEST)' 하나로 묶는다 (기획서 3.5) */
export type TopicType = 'CONTEST' | 'PROJECT' | 'STUDY' | 'ETC';

/** 대회 형식 — CONTEST 안에서 공모전/해커톤을 가르는 필수 태그 (기획서 2.4) */
export type ContestFormat = 'COMPETITION' | 'HACKATHON';

/** 대회 분야 — 7종 필수 enum ('기타' 포함) (기획서 3.5) */
export type ContestTag =
  | '데이터'
  | '환경'
  | '핀테크'
  | 'UX'
  | 'AI'
  | '지역경제'
  | '기타';

/** 파티 상태 (기획서 2.1) */
export type PartyStatus = 'RECRUITING' | 'IN_PROGRESS' | 'COMPLETED';

/** 성취(Goal) 유형 — JOINED 상속의 discriminator (기획서 3.6) */
export type GoalType = 'PROJECT' | 'CONTEST' | 'CHECKLIST';

/** 성취 진행 상태 (기획서 3.6) */
export type GoalStatus = 'WANT' | 'IN_PROGRESS' | 'HOLD' | 'ACHIEVED';

/** 성취 출처 — 사용자 노출 문구는 '자기신고' / '플랫폼 자동기록' (기획서 2.5) */
export type GoalSource = 'SELF_REPORTED' | 'PLATFORM_VERIFIED';

/**
 * 회원 역할.
 * MEMBER = 일반 회원, HOST = 주최측(기업·기관), ADMIN = 관리자.
 * 대회 등록은 HOST·ADMIN 만 할 수 있다 (기획서 2.4).
 */
export type MemberRole = 'MEMBER' | 'HOST' | 'ADMIN';

/** 좋아요·북마크의 다형성 대상 (기획서 3.2) */
export type TargetType = 'PARTY' | 'CONTEST' | 'GOAL';

export interface Paginated<T> {
  items: T[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

/* ---------- 사용자 / 프로필 ---------- */

export interface UserSummary {
  id: ID;
  name: string;
  /** 아바타 대체용 이니셜(한 글자) — 프로필 사진이 없을 때 쓴다 */
  initial: string;
  /** 프로필 사진. 없으면 initial 이니셜 아바타로 대체된다 */
  avatarUrl?: string;
  role: string;
}

export interface AchievementLink {
  label: string;
  url: string;
}

export interface Achievement {
  id: ID;
  /** 목표 유형 (discriminator) */
  type: GoalType;
  /** 진행 상태 — ACHIEVED 는 종료 상태로 이후 전이 불가 */
  status: GoalStatus;
  /** 자기신고 / 플랫폼 자동기록 */
  source: GoalSource;
  year: string;
  period: string;
  title: string;
  description: string;
  tags: string[];
  links: AchievementLink[];
  /** 상세 조회 시마다 증가 (기획서 3.2) */
  viewCount: number;
  /** PLATFORM_VERIFIED 일 때만 값 존재 — 좋아요·조회수를 원본 파티로 합산 (기획서 3.2) */
  sourcePartyId?: ID;
}

export interface CareerItem {
  id: ID;
  period: string;
  title: string;
  org: string;
  description: string;
}

export interface ProfileLink {
  id: ID;
  label: string;
  url: string;
}

export interface BadgeItem {
  id: ID;
  icon: string;
  label: string;
  earned: boolean;
}

export interface UserProfile extends UserSummary {
  /** GitHub 사용자명 — 팀 스페이스의 커밋 작성자를 회원과 연결하는 데 쓴다 */
  githubUsername?: string;
  /**
   * 계정 권한. UserSummary.role 은 화면에 보여주는 대표 포지션 문구이고,
   * 이 값은 등록 권한을 가르는 계정 역할이라 서로 다른 개념이다.
   */
  memberRole: MemberRole;
  bio: string;
  /** 대표 포지션 — 4종 고정 enum */
  position: PositionType;
  skills: string[];
  stats: {
    completedParties: number;
    awards: number;
    exhibitions: number;
    approvalRate: number;
  };
  streakDays: number;
  badges: BadgeItem[];
  achievements: Achievement[];
  careers: CareerItem[];
  links: ProfileLink[];
}

/* ---------- 파티(팀 모집) ---------- */

export interface PartyPosition {
  type: PositionType;
  /** 정원 — 이미 승인된 filledCount 보다 낮게 수정할 수 없다 (기획서 2.1) */
  capacity: number;
  filledCount: number;
}

export interface Party {
  id: ID;
  title: string;
  summary: string;
  /** 주제 유형 — 공모전·해커톤은 CONTEST 하나로 묶인다 */
  topicType: TopicType;
  /** topicType 이 CONTEST 일 때, 연동된 대회의 형식 (목록 카드 표기용) */
  contestFormat?: ContestFormat;
  /** 크루온에 등록된 대회와 연결된 경우에만 값 존재 (기획서 3.5) */
  contestId?: ID;
  /** 외부 대회는 이름을 직접 입력 — 허브 노출만 없고 모집글은 정상 생성 */
  contestName?: string;
  /** 원본 대회 페이지 링크 — 등록 여부와 무관하게 항상 제공 */
  contestLinkUrl?: string;
  /** 분야 — 기획서상 PARTY 에는 없는 필드이나 목업 필터 유지를 위해 잠정 보존 */
  subCategory: string;
  positions: PartyPosition[];
  applicants: number;
  dday: string;
  deadline: string;
  createdAt: string;
  leader: UserSummary;
  tags: string[];
  matchScore?: number;
  /** 홈 TOP3 · 인기순 정렬 기준 (기획서 2.1) */
  likeCount: number;
  viewCount: number;
  likedByMe?: boolean;
  bookmarkedByMe?: boolean;
  status: PartyStatus;
}

export interface PartyDetail extends Party {
  description: string;
  schedule: string;
  meetingType: string;
  members: UserSummary[];
  requirements: string[];
}

export type ApplicantStatus = 'pending' | 'accepted' | 'rejected';

export interface Applicant {
  id: ID;
  partyId: ID;
  partyName: string;
  position: PositionType;
  /** 지원자 프로필의 출처 배지 */
  source: GoalSource;
  user: UserSummary;
  appliedAt: string;
  status: ApplicantStatus;
  message: string;
  skills: string[];
  achievements: string[];
}

/* ---------- 공모전 ---------- */

export interface Contest {
  id: ID;
  title: string;
  host: string;
  /** 주최측 계정(HostCompany) id. 이 값이 내 소속과 같아야 수정·삭제할 수 있다 */
  hostId?: ID;
  /** 공모전 / 해커톤 — 필수 태그 (기획서 2.4) */
  format: ContestFormat;
  /** 분야 — 7종 필수 enum (기획서 3.5) */
  tag: ContestTag;
  status: '접수중' | '마감임박' | '마감';
  prize: string;
  dday: string;
  period: string;
  /** 원본 페이지 링크 — 필수 (기획서 3.5) */
  linkUrl: string;
  /** 등록한 대표 사진. 없으면 카드 상단이 기본 빗금 배경으로 보인다 */
  coverImageUrl?: string;
  viewCount: number;
  likeCount: number;
  likedByMe?: boolean;
  bookmarkedByMe?: boolean;
  teams: number;
}

export interface ContestDetail extends Contest {
  description: string;
  target: string;
  relatedParties: Party[];
}

/* ---------- 전시관 ---------- */

/** 전시 상세 댓글 — 커밋 댓글과 같은 구조를 쓴다 */
export type ExhibitionComment = ThreadComment;

export interface ExhibitionProject {
  id: ID;
  title: string;
  summary: string;
  partyName: string;
  role: PositionType;
  category: string;
  source: GoalSource;
  skills: string[];
  viewCount: number;
  likeCount: number;
  likedByMe?: boolean;
  bookmarkedByMe?: boolean;
  /** 등록한 대표 사진. 없으면 카드 상단이 기본 빗금 배경으로 보인다 */
  coverImageUrl?: string;
  /** PLATFORM_VERIFIED 면 좋아요·조회수를 이 파티로 합산한다 (기획서 3.2) */
  sourcePartyId?: ID;
  leader: UserSummary;
  thumbnailLabel: string;
}

export interface ExhibitionDetail extends ExhibitionProject {
  description: string;
  members: UserSummary[];
  links: ProfileLink[];
  period: string;
  comments: ThreadComment[];
}

/* ---------- 팀 스페이스 ---------- */

/** 개인 TODO(성취 CHECKLIST 타입)의 항목 상태 */
export type ChecklistState = 'open' | 'requested' | 'done';

export interface ChecklistItem {
  id: ID;
  content: string;
  state: ChecklistState;
  assignee: string | null;
  approvals: number;
  quorum: number;
}

/**
 * 댓글 — 커밋(팀 협업)과 전시 상세에서 함께 쓴다.
 *
 * 서버는 (target_id, 작성자, 내용, parentCommentId, 작성일시) 로 평평하게 저장하고,
 * 화면은 원댓글 아래 답글을 묶어 그리므로 여기서는 중첩 형태로 다룬다.
 * 답글의 답글은 없다 — 깊이는 1단계로 제한된다 (기획서 3.8).
 */
export interface ThreadComment {
  id: ID;
  authorName: string;
  authorInitial: string;
  content: string;
  createdAt: string;
  /** 원댓글에만 값이 있다. 답글은 항상 빈 배열이다. */
  replies: ThreadComment[];
}

/**
 * GitHub 웹훅(push 이벤트)으로 받아오는 커밋 한 건.
 *
 * 저장 스키마(ERD)는 아직 미정이라, push payload 의 commits[] 에서 바로 뽑을 수 있는
 * 값만 골라 담았다 — 해시 · 제목 · 작성자 · 시각 · 브랜치 · 변경량 · 원본 링크.
 */
export interface TeamCommit {
  id: ID;
  /** 짧은 해시 (7자리) */
  sha: string;
  /** 커밋 메시지 첫 줄 */
  message: string;
  authorName: string;
  authorInitial: string;
  /** GitHub username — 회원 프로필의 githubUsername 과 맞춰 크루온 계정에 연결한다 */
  githubUsername: string;
  /** 매칭된 크루온 회원 id (매칭 실패 시 없음) */
  memberId?: ID;
  /** 그룹 헤더로 쓰는 날짜 (YYYY.MM.DD) */
  date: string;
  /** 화면에 보여줄 시각 (HH:mm) */
  time: string;
  branch: string;
  additions: number;
  deletions: number;
  changedFiles: number;
  url: string;
  /** 동료 승인 — 정족수를 채우면 approved 로 바뀐다 */
  approvalState: 'pending' | 'approved';
  approvals: number;
  quorum: number;
  /** 승인한 팀원 이름 */
  approvers: string[];
}

export interface TeamSpace {
  id: ID;
  partyId: ID;
  title: string;
  contestName?: string;
  period: string;
  members: UserSummary[];
  /** 커밋 완료 승인에 필요한 인원 수 (최소 1, 상한은 팀 인원 수) */
  commitQuorum: number;
  /** 커밋 id → 댓글 목록 */
  threads: Record<ID, ThreadComment[]>;
}

/* ---------- 라이브 채팅 ---------- */

/**
 * 채팅방 종류.
 *
 * - PARTY  : 파티원 전체가 들어가는 팀 채팅방. 매칭이 끝나면 자동 생성된다
 * - DIRECT : 같은 파티 팀원끼리의 1:1 채팅방. 팀 채팅에서 개인 대화로 넘어갈 때 쓴다
 *
 * 매칭 전 단계(모집자 ↔ 지원자)는 채팅이 아니라 쪽지(2.8)가 담당한다 —
 * 아직 팀원이 아닌 사람들끼리 채팅방이 무분별하게 생기는 것을 막기 위해서다.
 */
export type ChatRoomType = 'PARTY' | 'DIRECT';

export interface ChatMessage {
  id: ID;
  roomId: ID;
  senderId: ID;
  senderName: string;
  senderInitial: string;
  content: string;
  /** 날짜 구분선 키 (YYYY.MM.DD) */
  date: string;
  /** 말풍선에 표시할 시각 (HH:mm) */
  sentAt: string;
  /** 내가 보낸 메시지인지 — 서버가 senderId 와 로그인 사용자를 비교해 내려준다 */
  mine: boolean;
  /** 전송 중 · 실패 상태 (낙관적 렌더링용, 서버 저장값 아님) */
  pending?: boolean;
  failed?: boolean;
}

export interface ChatRoom {
  id: ID;
  type: ChatRoomType;
  /** 소속 파티 id — DIRECT 방도 어느 파티에서 파생됐는지 따라간다 */
  targetId: ID;
  title: string;
  /**
   * 참여자 목록.
   * 별도 참여자 테이블을 두지 않고 PARTY_MEMBER(state=APPROVED) 에서 파생한다.
   */
  participants: UserSummary[];
  /** DIRECT 방의 상대방 (PARTY 방에는 없음) */
  counterpart?: UserSummary;
  /** 목록에 표시할 마지막 메시지 미리보기 */
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount?: number;
}

/**
 * 내가 속한 팀 하나의 채팅 묶음.
 * 방은 '형성된 팀'을 기준으로만 생기므로, 팀이 없으면 채팅도 없다.
 */
export interface TeamChatGroup {
  partyId: ID;
  partyTitle: string;
  teamRoom: ChatRoom;
  /** 나를 제외한 참여자 — 누르면 1:1 방이 열린다 */
  members: UserSummary[];
  /** 이미 열려 있는 1:1 방 */
  directRooms: ChatRoom[];
}

/* ---------- 알림 / 쪽지 ---------- */

export type NotificationType =
  | 'approval'
  | 'applicant'
  | 'deadline'
  | 'message'
  | 'achievement'
  | 'contest'
  | 'comment';

export type NotificationTarget =
  | 'team'
  | 'detail'
  | 'mypageManage'
  | 'mypageMessages'
  | 'mypageIdentity'
  | 'contests'
  | 'mypageBookmarks';

export interface AppNotification {
  id: ID;
  type: NotificationType;
  text: string;
  time: string;
  unread: boolean;
  target: NotificationTarget;
}

export interface MessageReply {
  id: ID;
  from: string;
  text: string;
  time: string;
  mine: boolean;
}

export interface DirectMessage {
  id: ID;
  from: string;
  role: string;
  initial: string;
  time: string;
  unread: boolean;
  text: string;
  replies: MessageReply[];
}

/* ---------- 북마크 ---------- */

/**
 * 북마크는 파티·대회·성취를 targetType + targetId 로 가리키는 다형성 구조라(기획서 3.2),
 * 마이페이지 북마크함도 대상 구분 없이 한 목록으로 보여준다 (기획서 2.11).
 */
export interface BookmarkItem {
  id: ID;
  targetType: TargetType;
  targetId: ID;
  title: string;
  subtitle: string;
  /** 카드 우측 보조 정보 — D-day / 상금 / 조회수 등 대상별로 다르다 */
  meta: string;
  tags: string[];
  createdAt: string;
}

/* ---------- TODO ---------- */

/**
 * 개인 TODO — 실체는 성취(Goal)의 CHECKLIST 타입이다 (기획서 2.5 체크리스트형 목표).
 * 화면 문구만 '개인 TODO'로 유지한다.
 */
export interface TodoItem {
  id: ID;
  title: string;
  category: string;
  createdAt: string;
  status: GoalStatus;
  totalCount: number;
  doneCount: number;
}

/* ---------- 주최측 ---------- */

export interface HostCompany {
  id: ID;
  name: string;
  bizNumber: string;
  verified: boolean;
  manager: string;
  email: string;
  phone: string;
  homepage: string;
  intro: string;
}

export interface HostContestSummary extends Contest {
  /** 연동된 파티 수 */
  linkedParties: number;
  submissions: number;
}

/* ---------- 관리자 ---------- */

export interface AdminStat {
  label: string;
  value: string;
  delta: string;
}

export interface ReportItem {
  id: ID;
  type: string;
  target: string;
  reporter: string;
  reason: string;
  createdAt: string;
  status: '대기' | '처리완료' | '반려';
}

export interface HostApproval {
  id: ID;
  company: string;
  bizNumber: string;
  manager: string;
  requestedAt: string;
  status: '대기' | '승인' | '반려';
}

/* ---------- 인증 ---------- */

export interface AuthUser {
  id: ID;
  name: string;
  email: string;
  initial: string;
  role: MemberRole;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  password: string;
  nickname: string;
  position: PositionType;
  agreements: string[];
}

/* ---------- 홈 ---------- */

export interface HeroSlide {
  id: ID;
  tag?: string;
  headline: string;
  headlineGlow?: string;
  sub: string;
  actions: { label: string; href: string; variant: 'primary' | 'ghost' }[];
  art: { mark: string; sub: string; chips: string[] };
}
