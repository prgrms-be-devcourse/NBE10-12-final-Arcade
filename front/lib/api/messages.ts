/**
 * 쪽지 API — ApiV1MessageController 기준.
 *
 * 경로가 둘로 갈린다. 내 쪽지함은 `/members/me/messages`, 발송은 상대를 지목하는
 * `/members/{memberId}/messages` 다.
 *
 * 서버에는 '스레드'나 '답장' 개념이 없다. 쪽지는 보낸 사람 → 받는 사람 한 방향 기록이고,
 * 답장은 상대에게 새 쪽지를 보내는 것으로 표현한다.
 */
import type { DirectMessage, ID } from '@/lib/types';
import { MOCK_MESSAGES } from '@/lib/mock';
import { ApiError, USE_MOCK, http, mockResponse } from './client';
import { timeAgo } from './time';

/** 쪽지함 구분 — 받은 쪽지 / 보낸 쪽지 (서버 MessageFilterOption) */
export type MessageBoxName = 'RECEIVED' | 'SENT';

/** 백엔드 MessageMemberDto */
interface MessageMemberResponse {
  id: number;
  name: string;
  nickname: string | null;
}

/** 백엔드 MessageListDto — contentPreview 에 본문이 그대로 들어 있다 */
interface MessageListResponse {
  id: number;
  sender: MessageMemberResponse;
  recipient: MessageMemberResponse;
  contentPreview: string;
  isRead: boolean;
  createAt: string;
}

/** 백엔드 MessagePageDto */
interface MessagePageResponse {
  content: MessageListResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 백엔드 MessageDto — 발송·읽음 처리 응답 */
interface MessageResponse {
  id: number;
  senderId: number;
  recipientId: number;
  content: string;
  isRead: boolean;
  createAt: string;
}

function displayName(member: MessageMemberResponse): string {
  return member.nickname ?? member.name;
}

/**
 * MessageListDto → DirectMessage.
 *
 * 화면은 '상대가 누구인가'로 목록을 읽는다. 받은함이면 보낸 사람이, 보낸함이면 받는 사람이 상대다.
 *
 * 서버에 없어서 비워 두는 값:
 * - role    : 회원의 대표 포지션. 쪽지 응답에는 이름만 온다
 * - replies : 답장 스레드가 없다. 답장은 상대에게 보내는 새 쪽지가 된다
 */
function toDirectMessage(dto: MessageListResponse, box: MessageBoxName): DirectMessage {
  const counterpart = box === 'SENT' ? dto.recipient : dto.sender;
  const name = displayName(counterpart);

  return {
    id: String(dto.id) as ID,
    counterpartId: String(counterpart.id) as ID,
    from: name,
    role: '',
    initial: name.charAt(0) || '?',
    time: timeAgo(dto.createAt),
    // 보낸 쪽지의 읽음 표시는 '상대가 읽었는지'라 목록에서 안 읽음으로 강조하지 않는다
    unread: box === 'RECEIVED' && !dto.isRead,
    text: dto.contentPreview,
    replies: [],
  };
}

/** 서버로 보낼 id 만 남긴다 — 중복은 400, 숫자가 아닌 값은 애초에 서버 id 가 아니다 */
function toServerIds(ids: string[]): number[] {
  return [...new Set(ids)].map(Number).filter((id) => Number.isFinite(id));
}

/**
 * GET /api/v1/members/me/messages — 내 쪽지함.
 *
 * box 로 받은함·보낸함을 가른다(기본 받은함). 최신순으로 페이지 단위로 온다.
 */
export async function fetchMessages(options?: {
  box?: MessageBoxName;
  page?: number;
  size?: number;
}): Promise<DirectMessage[]> {
  const box = options?.box ?? 'RECEIVED';

  // 목 데이터는 받은 쪽지만 있다. 보낸함은 빈 목록으로 둔다
  if (USE_MOCK) return mockResponse(box === 'RECEIVED' ? MOCK_MESSAGES : []);

  const result = await http.get<MessagePageResponse>('/members/me/messages', {
    query: { box, page: options?.page, size: options?.size },
  });

  return result.content.map((dto) => toDirectMessage(dto, box));
}

/**
 * 로그인하지 않았으면 빈 목록을 돌려주는 버전.
 * 마이페이지가 서버 컴포넌트에서 프로필과 함께 읽어서, 401 을 그대로 던지면 화면 전체가 죽는다.
 *
 * 지금은 서버 5xx 도 함께 삼킨다 — 쪽지 상대 중 한 명이라도 닉네임이 비어 있으면 목록 조회가
 * NullPointerException 500 으로 죽기 때문이다(docs/프론트-API연동_백엔드_수정요청.md ⑨).
 * 그대로 두면 쪽지 하나 때문에 마이페이지 전체를 못 연다. 서버가 고쳐지면 401 만 잡도록 되돌릴 것.
 */
export async function fetchMessagesOrEmpty(options?: {
  box?: MessageBoxName;
  page?: number;
  size?: number;
}): Promise<DirectMessage[]> {
  try {
    return await fetchMessages(options);
  } catch (error) {
    if (error instanceof ApiError && (error.status === 401 || error.status >= 500)) return [];
    throw error;
  }
}

/**
 * POST /api/v1/members/{memberId}/messages — 특정 회원에게 쪽지를 보낸다 (기획서 2.8).
 *
 * 사전 관계(친구·팔로우)를 요구하지 않는다. 전시나 프로필을 보고 바로 연락하는 게 이 기능의 쓰임이다.
 * 실패해도 화면이 안내 문구를 그대로 띄울 수 있게 예외 대신 결과를 돌려준다.
 */
export async function sendDirectMessage(payload: {
  recipientId: string;
  content: string;
}): Promise<{ sent: boolean; message: string }> {
  const content = payload.content.trim();
  if (!content) return { sent: false, message: '내용을 입력해 주세요.' };

  if (USE_MOCK) {
    return mockResponse({
      sent: true,
      message: '쪽지를 보냈어요. 상대가 확인하면 알림으로 알려드릴게요.',
    });
  }

  const recipientId = Number(payload.recipientId);
  if (!Number.isInteger(recipientId) || recipientId <= 0) {
    return { sent: false, message: '상대 회원 정보를 찾을 수 없어요.' };
  }

  try {
    await http.post<MessageResponse>(`/members/${recipientId}/messages`, { content });
    return { sent: true, message: '쪽지를 보냈어요. 상대가 확인하면 알림으로 알려드릴게요.' };
  } catch (error) {
    // 서버 예외는 msg 를 그대로 화면 문구로 쓸 수 있는 봉투로 온다 (없는 회원 404, 1000자 초과 400 등)
    return {
      sent: false,
      message:
        error instanceof ApiError ? error.message : '쪽지를 보내지 못했어요. 잠시 후 다시 시도해 주세요.',
    };
  }
}

/**
 * PATCH /api/v1/members/me/messages — 읽음 처리.
 *
 * 알림과 마찬가지로 개별·전체 구분 없이 id 배열 하나로 받는다.
 * 받은 쪽지만 읽음 처리할 수 있다(내가 보낸 쪽지는 상대가 읽는다).
 */
export async function markMessagesRead(ids: string[]): Promise<void> {
  const targets = toServerIds(ids);
  if (targets.length === 0) return;
  if (USE_MOCK) return mockResponse(undefined as void);

  await http.patch<MessageResponse[]>('/members/me/messages', { ids: targets });
}
