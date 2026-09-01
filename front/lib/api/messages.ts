import type { DirectMessage, MessageReply } from '@/lib/types';
import { MOCK_MESSAGES } from '@/lib/mock';
import { http, mockResponse } from './client';

/**
 * 백엔드에 대응 엔드포인트가 아직 없는 모듈이다.
 *
 * 없는 경로로 요청해도 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다.
 * 그래서 실제 API 가 생기기 전까지는 이 상수로 데모 데이터만 쓰도록 고정한다.
 *
 * 서버가 준비되면 이 상수를 지우고 client 의 USE_MOCK 을 다시 import 하면 아래 http 호출이 살아난다.
 */
const USE_MOCK: boolean = true;


/** GET /messages */
export async function fetchMessages(): Promise<DirectMessage[]> {
  if (USE_MOCK) return mockResponse(MOCK_MESSAGES);
  return http.get<DirectMessage[]>('/messages');
}

/**
 * POST /messages — 특정 회원에게 쪽지를 보낸다 (기획서 2.8).
 *
 * 사전 관계(친구·팔로우)를 요구하지 않는다. 전시나 프로필을 보고 바로 연락하는 게
 * 이 기능의 주된 쓰임이기 때문이다.
 */
export async function sendDirectMessage(payload: {
  recipientId: string;
  content: string;
}): Promise<{ sent: boolean; message: string }> {
  if (USE_MOCK) {
    const valid = payload.content.trim().length > 0;
    return mockResponse({
      sent: valid,
      message: valid
        ? '쪽지를 보냈어요. 상대가 확인하면 알림으로 알려드릴게요.'
        : '내용을 입력해 주세요.',
    });
  }
  return http.post('/messages', payload);
}

/** PATCH /messages/{id}/read */
export async function markMessageRead(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/messages/${id}/read`);
}

/** PATCH /messages/read-all */
export async function markAllMessagesRead(): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>('/messages/read-all');
}

/** POST /messages/{id}/replies */
export async function replyToMessage(id: string, text: string): Promise<MessageReply> {
  if (USE_MOCK) {
    return mockResponse({ id: `r-${Date.now()}`, from: '정하늘', text, time: '방금 전', mine: true });
  }
  return http.post<MessageReply>(`/messages/${id}/replies`, { text });
}
