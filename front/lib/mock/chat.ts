import type { ChatMessage, ChatRoom } from '@/lib/types';
import { MOCK_USER_SUMMARIES } from './users';

/**
 * 채팅방 데모 데이터.
 *
 * 참여자는 파티원(PARTY_MEMBER state=APPROVED)에서 파생되므로 별도 관계 테이블이 없다.
 * 1:1(DIRECT) 방도 같은 파티 팀원끼리만 열리므로 친구·팔로우 개념이 필요 없다.
 */

/** 파티별 팀 채팅방 */
export const MOCK_TEAM_ROOMS: Record<string, ChatRoom> = {
  paybridge: {
    id: 'room-paybridge',
    type: 'PARTY',
    targetId: 'paybridge',
    title: '페이브릿지 해커톤 도전팀',
    participants: [MOCK_USER_SUMMARIES.haneul, MOCK_USER_SUMMARIES.somin],
    lastMessage: '넵 지금 볼게요. 내일 발표자료도 같이 정리해요',
    lastMessageAt: '09:31',
  },
  oakroom: {
    id: 'room-oakroom',
    type: 'PARTY',
    targetId: 'oakroom',
    title: '프로그래머스 오락실 공모전 참여하실분',
    participants: [
      MOCK_USER_SUMMARIES.haneul,
      MOCK_USER_SUMMARIES.doyoon,
      MOCK_USER_SUMMARIES.yerin,
    ],
    lastMessage: '무드보드 공유드렸어요. 확인 후 의견 주세요!',
    lastMessageAt: '10:05',
  },
};

/** 파티별 1:1 방 — 팀 채팅에서 개인 대화로 넘어갈 때 쓴다 */
export const MOCK_DIRECT_ROOMS: Record<string, ChatRoom[]> = {
  paybridge: [
    {
      id: 'dm-paybridge-somin',
      type: 'DIRECT',
      targetId: 'paybridge',
      title: '윤소민',
      participants: [MOCK_USER_SUMMARIES.haneul, MOCK_USER_SUMMARIES.somin],
      counterpart: MOCK_USER_SUMMARIES.somin,
      lastMessage: '그 부분은 제가 따로 정리해서 드릴게요',
      lastMessageAt: '어제',
      unreadCount: 1,
    },
  ],
  oakroom: [
    {
      id: 'dm-oakroom-doyoon',
      type: 'DIRECT',
      targetId: 'oakroom',
      title: '김도윤',
      participants: [MOCK_USER_SUMMARIES.haneul, MOCK_USER_SUMMARIES.doyoon],
      counterpart: MOCK_USER_SUMMARIES.doyoon,
      lastMessage: '스크럼 시간 30분만 미룰 수 있을까요?',
      lastMessageAt: '2일 전',
    },
    {
      id: 'dm-oakroom-yerin',
      type: 'DIRECT',
      targetId: 'oakroom',
      title: '서예린',
      participants: [MOCK_USER_SUMMARIES.haneul, MOCK_USER_SUMMARIES.yerin],
      counterpart: MOCK_USER_SUMMARIES.yerin,
    },
  ],
};

export const MOCK_CHAT_MESSAGES: Record<string, ChatMessage[]> = {
  'room-paybridge': [
    {
      id: 'm1',
      roomId: 'room-paybridge',
      senderId: 'somin',
      senderName: '윤소민',
      senderInitial: '윤',
      content: '대시보드 응답 스펙 바뀐 거 반영했어요. 커밋 올려뒀습니다!',
      date: '2026.08.24',
      sentAt: '15:09',
      mine: false,
    },
    {
      id: 'm2',
      roomId: 'room-paybridge',
      senderId: 'haneul',
      senderName: '정하늘',
      senderInitial: '정',
      content: '확인했습니다. totalAmount 로 맞춰둘게요.',
      date: '2026.08.24',
      sentAt: '15:22',
      mine: true,
    },
    {
      id: 'm3',
      roomId: 'room-paybridge',
      senderId: 'haneul',
      senderName: '정하늘',
      senderInitial: '정',
      content: '슬랙 알림도 붙였어요. 승인 부탁드려요 🙏',
      date: '2026.08.24',
      sentAt: '18:44',
      mine: true,
    },
    {
      id: 'm4',
      roomId: 'room-paybridge',
      senderId: 'somin',
      senderName: '윤소민',
      senderInitial: '윤',
      content: '넵 지금 볼게요. 내일 발표자료도 같이 정리해요',
      date: '2026.08.25',
      sentAt: '09:31',
      mine: false,
    },
  ],
  'dm-paybridge-somin': [
    {
      id: 'd1',
      roomId: 'dm-paybridge-somin',
      senderId: 'haneul',
      senderName: '정하늘',
      senderInitial: '정',
      content: '소민님, 발표 때 데모 시연은 어느 화면으로 갈까요?',
      date: '2026.08.25',
      sentAt: '21:04',
      mine: true,
    },
    {
      id: 'd2',
      roomId: 'dm-paybridge-somin',
      senderId: 'somin',
      senderName: '윤소민',
      senderInitial: '윤',
      content: '그 부분은 제가 따로 정리해서 드릴게요',
      date: '2026.08.26',
      sentAt: '08:12',
      mine: false,
    },
  ],
  'room-oakroom': [
    {
      id: 'o1',
      roomId: 'room-oakroom',
      senderId: 'haneul',
      senderName: '정하늘',
      senderInitial: '정',
      content: '합류 환영합니다! 이번 주 목요일 저녁에 첫 스크럼 어떠세요?',
      date: '2026.08.26',
      sentAt: '20:12',
      mine: true,
    },
    {
      id: 'o2',
      roomId: 'room-oakroom',
      senderId: 'doyoon',
      senderName: '김도윤',
      senderInitial: '김',
      content: '좋습니다. 그때까지 픽셀 UI 컴포넌트 초안 잡아둘게요.',
      date: '2026.08.26',
      sentAt: '20:20',
      mine: false,
    },
    {
      id: 'o3',
      roomId: 'room-oakroom',
      senderId: 'yerin',
      senderName: '서예린',
      senderInitial: '서',
      content: '무드보드 공유드렸어요. 확인 후 의견 주세요!',
      date: '2026.08.27',
      sentAt: '10:05',
      mine: false,
    },
  ],
  'dm-oakroom-doyoon': [
    {
      id: 'do1',
      roomId: 'dm-oakroom-doyoon',
      senderId: 'doyoon',
      senderName: '김도윤',
      senderInitial: '김',
      content: '스크럼 시간 30분만 미룰 수 있을까요?',
      date: '2026.08.25',
      sentAt: '19:40',
      mine: false,
    },
  ],
  'dm-oakroom-yerin': [],
};
