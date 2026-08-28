import type { AppNotification } from '@/lib/types';
import { MOCK_NOTIFICATIONS } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

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
