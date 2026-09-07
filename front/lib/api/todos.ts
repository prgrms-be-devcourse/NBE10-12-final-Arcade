import type { ChecklistItem, GoalStatus, TodoItem } from '@/lib/types';
import { MOCK_SOLO_SPACES, MOCK_TODOS } from '@/lib/mock';
import { todoCategoryLabel } from '@/lib/constants';
import { USE_MOCK, http, mockResponse } from './client';

/** 개인 TODO 상세 화면(솔로 팀 스페이스)이 쓰는 모양. */
export interface SoloSpaceData {
  id: string;
  title: string;
  /** 화면에 보여줄 분류 문구. 서버 TodoCategory 를 한글로 옮긴 값이다 */
  type: string;
  createdAt: string;
  memo: string;
  checklist: ChecklistItem[];
}

/** 서버 PersonalTodoDto — 목록·등록·수정 응답 */
interface PersonalTodoResponse {
  id: number;
  ownerId: number;
  title: string;
  /** STUDY · SIDE · CAREER · CERTIFICATE · ETC */
  category: string;
  memo?: string;
  status: GoalStatus;
  totalCount: number;
  doneCount: number;
  createDate: string;
  modifyDate: string;
}

/** 서버 PersonalTodoItemDto */
interface PersonalTodoItemResponse {
  id: number;
  content: string;
  done: boolean;
  doneAt?: string;
  sortOrder: number;
}

/** 서버 PersonalTodoDetailDto — items 는 첫 페이지만 담긴다 */
interface PersonalTodoDetailResponse extends PersonalTodoResponse {
  items: PersonalTodoItemResponse[];
  hasMoreItems: boolean;
}

/** 서버 LocalDateTime 을 목록 '생성일' 칸 문구로 줄인다 (lib/api/time.ts 와 같은 표기) */
const toDateText = (value: string) => value.slice(0, 10).replace(/-/g, '.');

function toTodoItem(dto: PersonalTodoResponse): TodoItem {
  return {
    id: String(dto.id),
    title: dto.title,
    category: todoCategoryLabel(dto.category),
    createdAt: toDateText(dto.createDate),
    status: dto.status,
    totalCount: dto.totalCount,
    doneCount: dto.doneCount,
  };
}

/**
 * 개인 TODO 항목에는 담당자·승인 정족수 개념이 없다.
 * ChecklistItem 은 팀 체크리스트와 공유하는 타입이라 그 자리를 비워 둔다.
 */
function toChecklistItem(item: PersonalTodoItemResponse): ChecklistItem {
  return {
    id: String(item.id),
    content: item.content,
    state: item.done ? 'done' : 'open',
    assignee: null,
    approvals: 0,
    quorum: 0,
  };
}

/** GET /todos/me */
export async function fetchTodos(): Promise<TodoItem[]> {
  if (USE_MOCK) return mockResponse(MOCK_TODOS);

  // 서버가 Page 로 감싸 내려주므로 content 만 꺼낸다
  const page = await http.get<{ content: PersonalTodoResponse[] }>('/todos/me');
  return page.content.map(toTodoItem);
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
  return toTodoItem(await http.post<PersonalTodoResponse>('/todos', payload));
}

/** GET /todos/{id} — 개인 TODO 상세(솔로 팀 스페이스) */
export async function fetchSoloSpace(id: string): Promise<SoloSpaceData> {
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
  const dto = await http.get<PersonalTodoDetailResponse>(`/todos/${id}`);
  return {
    id: String(dto.id),
    title: dto.title,
    type: todoCategoryLabel(dto.category),
    createdAt: toDateText(dto.createDate),
    memo: dto.memo ?? '',
    checklist: dto.items.map(toChecklistItem),
  };
}

/** PATCH /todos/{id} — 부분 수정이라 바뀐 값만 보낸다 (null 인 항목은 서버가 그대로 둔다). */
export async function saveSoloMemo(id: string, memo: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch(`/todos/${id}`, { memo });
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
  return toChecklistItem(
    await http.post<PersonalTodoItemResponse>(`/todos/${id}/items`, { content }),
  );
}

/**
 * POST /todos/{id}/items/{itemId}/complete — 할 일 완료.
 *
 * 서버에 되돌리기 API 가 없다(화면에도 되돌리기 UI 가 없다). 그래서 해제는 아무것도 하지 않는다.
 */
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
  await http.patch(`/todos/${id}`, { status: 'ACHIEVED' });
}
