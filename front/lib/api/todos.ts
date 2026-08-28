import type { ChecklistItem, TodoItem } from '@/lib/types';
import { MOCK_SOLO_SPACES, MOCK_TODOS } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

/** GET /me/todos */
export async function fetchTodos(): Promise<TodoItem[]> {
  if (USE_MOCK) return mockResponse(MOCK_TODOS);
  return http.get<TodoItem[]>('/me/todos');
}

/** POST /me/todos */
export async function createTodo(payload: {
  title: string;
  category: string;
  memo?: string;
}): Promise<TodoItem> {
  if (USE_MOCK) {
    const id = `todo-${Date.now()}`;
    // 만든 직후 상세로 이동하므로, 목 저장소에도 넣어 둬야 그 화면에서 찾을 수 있다.
    MOCK_SOLO_SPACES[id] = {
      id,
      title: payload.title,
      type: payload.category,
      createdAt: '방금 생성',
      memo: payload.memo ?? '',
      checklist: [],
    };
    return mockResponse({
      id,
      title: payload.title,
      category: payload.category,
      createdAt: '방금 생성',
      status: 'WANT',
      totalCount: 0,
      doneCount: 0,
    });
  }
  return http.post<TodoItem>('/me/todos', payload);
}

/** GET /me/todos/{id} — 개인 TODO 상세(솔로 팀 스페이스) */
export async function fetchSoloSpace(id: string) {
  if (USE_MOCK) {
    const found = MOCK_SOLO_SPACES[id];
    if (found) return mockResponse(found);
    // 알 수 없는 id 는 빈 목록으로 돌려준다 (다른 항목 내용을 잘못 보여주지 않도록)
    return mockResponse({
      id,
      title: '새 개인 TODO',
      type: '기타',
      createdAt: '방금 생성',
      memo: '',
      checklist: [],
    });
  }
  return http.get(`/me/todos/${id}`);
}

/** PUT /me/todos/{id}/memo */
export async function saveSoloMemo(id: string, memo: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.put<void>(`/me/todos/${id}/memo`, { memo });
}

/** POST /me/todos/{id}/items */
export async function createSoloItem(id: string, content: string): Promise<ChecklistItem> {
  if (USE_MOCK) {
    return mockResponse({
      id: `solo-${Date.now()}`,
      content,
      state: 'open',
      assignee: '정하늘',
      approvals: 0,
      quorum: 0,
    });
  }
  return http.post<ChecklistItem>(`/me/todos/${id}/items`, { content });
}

/** PATCH /me/todos/{id}/items/{itemId} */
export async function toggleSoloItem(
  id: string,
  itemId: string,
  done: boolean,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.patch<void>(`/me/todos/${id}/items/${itemId}`, { done });
}

/** POST /me/todos/{id}/finish */
export async function finishTodo(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  return http.post<void>(`/me/todos/${id}/finish`);
}
