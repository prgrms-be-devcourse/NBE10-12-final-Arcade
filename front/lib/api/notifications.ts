import type { AppNotification } from '@/lib/types';
import { MOCK_NOTIFICATIONS } from '@/lib/mock';
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


/** GET /notifications */
export async function fetchNotifications(): Promise<AppNotification[]> {
  if (USE_MOCK) return mockResponse(MOCK_NOTIFICATIONS);
  return http.get<AppNotification[]>('/notifications');
}

/** PATCH /notifications/{id}/read */
export async function markNotificationRead(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/notifications/${id}/read`);
}

/** PATCH /notifications/read-all */
export async function markAllNotificationsRead(): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>('/notifications/read-all');
}

/** DELETE /notifications  { ids: [...] } — 선택 삭제 */
export async function deleteNotifications(ids: string[]): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.delete<void>('/notifications', { body: { ids } } as never);
}
