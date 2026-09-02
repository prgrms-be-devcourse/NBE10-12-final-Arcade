/**
 * 알림 API — ApiV1NotificationController 기준.
 *
 * 서버는 목록을 페이지로 내려주고, 읽음·삭제는 둘 다 id 배열 하나로 받는다.
 * 개별/전체를 가르는 엔드포인트가 따로 없어서 화면이 대상 id 를 모아 보낸다.
 */
import type { AppNotification, ID, NotificationTarget, NotificationType } from '@/lib/types';
import { MOCK_NOTIFICATIONS } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';
import { timeAgo } from './time';

/** 백엔드 NotificationType. 지금은 파티 지원 승인 한 종류뿐이고 앞으로 늘어난다 */
export type ServerNotificationType = 'PARTY_APPLICATION_APPROVED';

/** 백엔드 NotificationDto */
export interface NotificationResponse {
  id: number;
  type: ServerNotificationType;
  content: string;
  isRead: boolean;
  /** 생성 시각. 서버 필드명이 createAt 이다(값은 BaseEntity 의 createDate) */
  createAt: string;
}

/** 백엔드 NotificationPageDto */
export interface NotificationPageResponse {
  content: NotificationResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 서버 타입 → 화면 아이콘 구분 */
const NOTIFICATION_TYPES: Record<ServerNotificationType, NotificationType> = {
  PARTY_APPLICATION_APPROVED: 'approval',
};

/**
 * 서버 타입 → 눌렀을 때 갈 곳.
 *
 * 승인 알림은 지원한 사람이 받으므로 마이페이지 관리 탭('내 지원')으로 보낸다.
 * 응답에 파티 id 가 없어 해당 파티 상세로 바로 보낼 수는 없다.
 */
const NOTIFICATION_TARGETS: Record<ServerNotificationType, NotificationTarget> = {
  PARTY_APPLICATION_APPROVED: 'mypageManage',
};

/**
 * NotificationDto → AppNotification.
 *
 * 서버 타입이 늘어나도 화면이 죽지 않도록, 모르는 타입은 기본 아이콘·기본 이동 위치로 떨어뜨린다.
 */
function toAppNotification(dto: NotificationResponse): AppNotification {
  return {
    id: String(dto.id) as ID,
    type: NOTIFICATION_TYPES[dto.type] ?? 'approval',
    text: dto.content,
    time: timeAgo(dto.createAt),
    unread: !dto.isRead,
    target: NOTIFICATION_TARGETS[dto.type] ?? 'mypageManage',
  };
}

/** 서버로 보낼 id 만 남긴다 — 중복은 400, 숫자가 아닌 값은 애초에 서버 id 가 아니다 */
function toServerIds(ids: string[]): number[] {
  return [...new Set(ids)].map(Number).filter((id) => Number.isFinite(id));
}

/**
 * GET /api/v1/notifications — 내 알림 목록.
 *
 * 최신순으로 페이지 단위로 온다. 알림 드롭다운은 첫 페이지만 쓰므로 기본값을 그대로 둔다.
 */
export async function fetchNotifications(options?: {
  /** 읽음 여부 필터. 생략하면 전부 */
  isRead?: boolean;
  page?: number;
  size?: number;
}): Promise<AppNotification[]> {
  if (USE_MOCK) return mockResponse(MOCK_NOTIFICATIONS);

  const result = await http.get<NotificationPageResponse>('/notifications', {
    query: { isRead: options?.isRead, page: options?.page, size: options?.size },
  });

  return result.content.map(toAppNotification);
}

/**
 * PATCH /api/v1/notifications/read — 읽음 처리.
 *
 * 개별·전체 구분 없이 id 배열로 보낸다. 하나라도 없는 id 가 섞이면 404 로 전부 실패하므로
 * 화면이 들고 있는 id 만 넘긴다.
 */
export async function markNotificationsRead(ids: string[]): Promise<void> {
  const targets = toServerIds(ids);
  if (targets.length === 0) return;
  if (USE_MOCK) return mockResponse(undefined as void);

  await http.patch<{ id: number; isRead: boolean }[]>('/notifications/read', { ids: targets });
}

/** DELETE /api/v1/notifications — 선택 삭제. 서버가 없는 id 는 조용히 넘기므로 재시도해도 안전하다 */
export async function deleteNotifications(ids: string[]): Promise<void> {
  const targets = toServerIds(ids);
  if (targets.length === 0) return;
  if (USE_MOCK) return mockResponse(undefined as void);

  await http.delete<void>('/notifications', { body: { ids: targets } });
}
