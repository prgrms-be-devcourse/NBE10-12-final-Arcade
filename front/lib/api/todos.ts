import type { ChecklistItem, TodoItem } from '@/lib/types';
import { MOCK_SOLO_SPACES, MOCK_TODOS } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

/** 개인 TODO 상세 API와 목 데이터에서 공통으로 사용하는 최소 필드다. */
interface SoloSpaceResponse {
  title: string;
  category?: string;
  type?: string;
  memo?: string;
  status?: string;
}

/**
 * 백엔드에 대응 엔드포인트가 아직 없는 모듈이다.
 *
 * 없는 경로로 요청해도 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다.
 * 그래서 실제 API 가 생기기 전까지는 이 상수로 데모 데이터만 쓰도록 고정한다.
 *
 * 서버가 준비되면 이 상수를 지우고 client 의 USE_MOCK 을 다시 import 하면 아래 http 호출이 살아난다.
 */
/** GET /todos/me */
export async function fetchTodos(): Promise<TodoItem[]> {
  if (USE_MOCK) return mockResponse(MOCK_TODOS);
  return http.get<TodoItem[]>('/todos/me');
}

/** POST /todos */
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
  return http.post<TodoItem>('/todos', payload);
}

/** GET /todos/{id} — 개인 TODO 상세(솔로 팀 스페이스) */
export async function fetchSoloSpace(id: string): Promise<SoloSpaceResponse> {
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
  return http.get<SoloSpaceResponse>(`/todos/${id}`);
}

/** PATCH /todos/{id} — 백엔드에는 메모 전용 API가 없어 기존 값을 함께 보낸다. */
export async function saveSoloMemo(id: string, memo: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  const todo = await fetchSoloSpace(id);
  await http.patch(`/todos/${id}`, {
    title: todo.title,
    category: todo.category ?? todo.type,
    memo,
    status: todo.status ?? 'WANT',
  });
}

/** POST /todos/{id}/items */
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
  return http.post<ChecklistItem>(`/todos/${id}/items`, { content });
}

/** PATCH /todos/{id}/items/{itemId} — 내용 수정 */
export async function toggleSoloItem(
  id: string,
  itemId: string,
  done: boolean,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  if (done) {
    await http.post<void>(`/todos/${id}/items/${itemId}/complete`);
  }
}

/** PATCH /todos/{id} — TODO 완료 상태 변경 */
export async function finishTodo(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  const todo = await fetchSoloSpace(id);
  await http.patch(`/todos/${id}`, {
    title: todo.title,
    category: todo.category ?? todo.type,
    memo: todo.memo ?? '',
    status: 'ACHIEVED',
  });
}
