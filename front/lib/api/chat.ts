import type { ChatMessage, ChatRoom, TeamChatGroup } from '@/lib/types';
import {
  MOCK_CHAT_MESSAGES,
  MOCK_CURRENT_USER_ID,
  MOCK_DIRECT_ROOMS,
  MOCK_TEAM_ROOMS,
} from '@/lib/mock';
import { API_BASE_URL, USE_MOCK, http, mockResponse } from './client';

/**
 * 파티 채팅.
 *
 * 매칭이 끝나면 팀 채팅방(PARTY)이 자동 생성되고, 팀원끼리는 1:1 방(DIRECT)을 열 수 있다.
 * 매칭 전 문의는 채팅이 아니라 쪽지가 담당한다 (기획서 2.8).
 *
 * 참여자 목록은 별도 테이블 없이 PARTY_MEMBER(state=APPROVED) 에서 파생하므로,
 * 저장이 필요한 건 방(CHAT_ROOM)과 메시지(CHAT_MESSAGE) 둘뿐이다.
 */

/**
 * GET /me/chat-rooms — 내가 속한 팀들의 채팅 묶음.
 *
 * 방은 매칭이 끝나 '형성된 팀'에만 생긴다. 팀 페이지에 들어가지 않아도
 * 어느 화면에서든 채팅을 열 수 있도록 전역 도크가 이 목록을 쓴다.
 */
export async function fetchMyChatRooms(): Promise<TeamChatGroup[]> {
  if (USE_MOCK) {
    const groups = Object.entries(MOCK_TEAM_ROOMS).map(([partyId, teamRoom]) => ({
      partyId,
      partyTitle: teamRoom.title,
      teamRoom,
      members: teamRoom.participants.filter((user) => user.id !== MOCK_CURRENT_USER_ID),
      directRooms: MOCK_DIRECT_ROOMS[partyId] ?? [],
    }));
    return mockResponse(groups);
  }
  return http.get<TeamChatGroup[]>('/me/chat-rooms');
}

/** GET /parties/{partyId}/chat-rooms — 팀 채팅방 + 팀원별 1:1 방 */
export async function fetchPartyChatRooms(partyId: string): Promise<ChatRoom[]> {
  if (USE_MOCK) {
    const team = MOCK_TEAM_ROOMS[partyId] ?? MOCK_TEAM_ROOMS.paybridge;
    const directs = MOCK_DIRECT_ROOMS[partyId] ?? [];
    return mockResponse([team, ...directs]);
  }
  return http.get<ChatRoom[]>(`/parties/${partyId}/chat-rooms`);
}

/**
 * POST /parties/{partyId}/chat-rooms/direct — 팀원과의 1:1 방을 연다.
 * 이미 있으면 그 방을 그대로 돌려준다 (같은 상대에게 방이 여러 개 생기지 않게).
 */
export async function openDirectChatRoom(
  partyId: string,
  memberId: string,
): Promise<ChatRoom> {
  if (USE_MOCK) {
    const existing = (MOCK_DIRECT_ROOMS[partyId] ?? []).find(
      (room) => room.counterpart?.id === memberId,
    );
    if (existing) return mockResponse(existing);
    const team = MOCK_TEAM_ROOMS[partyId] ?? MOCK_TEAM_ROOMS.paybridge;
    const counterpart = team.participants.find((user) => user.id === memberId);
    return mockResponse({
      id: `dm-${partyId}-${memberId}`,
      type: 'DIRECT' as const,
      targetId: partyId,
      title: counterpart?.name ?? '팀원',
      participants: team.participants.filter((user) => user.id === memberId),
      counterpart,
    });
  }
  return http.post<ChatRoom>(`/parties/${partyId}/chat-rooms/direct`, { memberId });
}

/** GET /chat-rooms/{roomId}/messages */
export async function fetchChatMessages(roomId: string): Promise<ChatMessage[]> {
  if (USE_MOCK) return mockResponse(MOCK_CHAT_MESSAGES[roomId] ?? []);
  return http.get<ChatMessage[]>(`/chat-rooms/${roomId}/messages`);
}

/**
 * POST /chat-rooms/{roomId}/messages
 *
 * 실시간 전달은 WebSocket 이 담당하지만, 전송 자체는 REST 로 두어
 * 소켓이 끊겨도 메시지가 유실되지 않게 한다. 서버는 저장 후 소켓으로 브로드캐스트한다.
 */
export async function sendChatMessage(roomId: string, content: string): Promise<ChatMessage> {
  if (USE_MOCK) {
    const now = new Date();
    const pad = (value: number) => String(value).padStart(2, '0');
    return mockResponse({
      id: `msg-${now.getTime()}`,
      roomId,
      senderId: 'haneul',
      senderName: '정하늘',
      senderInitial: '정',
      content,
      date: `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`,
      sentAt: `${pad(now.getHours())}:${pad(now.getMinutes())}`,
      mine: true,
    });
  }
  return http.post<ChatMessage>(`/chat-rooms/${roomId}/messages`, { content });
}

export type ChatSocketStatus = 'connecting' | 'open' | 'closed' | 'mock';

interface ChatSocketHandlers {
  onMessage: (message: ChatMessage) => void;
  onStatus?: (status: ChatSocketStatus) => void;
}

/**
 * 채팅방 WebSocket 구독. 반환값을 호출하면 연결을 닫는다.
 *
 * 목 모드에서는 가짜 수신 메시지를 만들지 않는다 — 없는 대화가 있는 것처럼 보이면
 * 데모를 보는 사람이 실제 동작으로 오해하기 때문이다. 상태만 'mock' 으로 알린다.
 */
export function connectChatSocket(
  roomId: string,
  { onMessage, onStatus }: ChatSocketHandlers,
): () => void {
  if (USE_MOCK || !API_BASE_URL) {
    onStatus?.('mock');
    return () => {};
  }

  const wsUrl = `${API_BASE_URL.replace(/^http/, 'ws')}/chat-rooms/${roomId}/subscribe`;
  onStatus?.('connecting');

  const socket = new WebSocket(wsUrl);
  socket.addEventListener('open', () => onStatus?.('open'));
  socket.addEventListener('close', () => onStatus?.('closed'));
  socket.addEventListener('message', (event) => {
    try {
      onMessage(JSON.parse(event.data as string) as ChatMessage);
    } catch {
      /* 파싱 실패한 프레임은 무시한다 */
    }
  });

  return () => socket.close();
}
