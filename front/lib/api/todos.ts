import type { ChecklistItem, GoalStatus, TodoItem } from '@/lib/types';
import { MOCK_SOLO_SPACES, MOCK_TODOS } from '@/lib/mock';
import { todoCategoryLabel } from '@/lib/constants';
import { USE_MOCK, http, mockResponse } from './client';
import { toDateText } from './time';

/** 개인 TODO 상세 화면(솔로 팀 스페이스)이 쓰는 모양. */
export interface SoloSpaceDetail {
  id: string;
  title: string;
  /** 화면에 보여줄 분류 문구. 서버 TodoCategory 를 한글로 옮긴 값이다 */
  type: string;
  createdAt: string;
  memo: string;
  /** 항목 수 · 완료 수. 성취 수정 화면의 연결 카드가 진행률로 쓴다 */
  totalCount: number;
  doneCount: number;
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

function toChecklistItem(item: PersonalTodoItemResponse): ChecklistItem {
  return {
    id: String(item.id),
    content: item.content,
    state: item.done ? 'done' : 'open',
  };
}

/**
 * GET /todos/me — 내 개인 TODO 목록.
 *
 * linked=false 를 주면 아직 성취에 연결되지 않은 것만 온다. 성취 등록·수정 화면의 TODO 선택은
 * 이미 연결된 것을 고르면 서버가 409 로 거절하므로 이 필터를 써야 한다.
 *
 * size 기본값은 20 이다. 선택 목록처럼 전부 필요한 곳은 최대치(100)를 준다.
 */
export async function fetchTodos(options?: {
  linked?: boolean;
  size?: number;
}): Promise<TodoItem[]> {
  if (USE_MOCK) return mockResponse(MOCK_TODOS);

  // 서버가 Page 로 감싸 내려주므로 content 만 꺼낸다
  const page = await http.get<{ content: PersonalTodoResponse[] }>('/todos/me', {
    query: { linked: options?.linked, size: options?.size },
  });
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

/**
 * GET /todos/{id}/items — 할 일 목록 한 페이지.
 * 서버가 Spring Page 를 그대로 내려줘 껍데기가 다르다(page 가 아니라 number). content 만 쓴다.
 */
const ITEMS_PAGE_SIZE = 100; // 서버 상한

async function fetchRemainingItems(id: string): Promise<PersonalTodoItemResponse[]> {
  const items: PersonalTodoItemResponse[] = [];
  // ponytail: 전부 받을 때까지 순차 요청. 항목이 수백 개면 왕복이 늘어난다 - 화면에 더보기가 붙으면 그때 페이지 단위로 바꾸면 된다
  for (let page = 0; ; page += 1) {
    const chunk = await http.get<{ content: PersonalTodoItemResponse[] }>(`/todos/${id}/items`, {
      query: { page, size: ITEMS_PAGE_SIZE },
    });
    items.push(...chunk.content);
    if (chunk.content.length < ITEMS_PAGE_SIZE) return items;
  }
}

/** GET /todos/{id} — 개인 TODO 상세(솔로 팀 스페이스) */
export async function fetchSoloSpace(id: string): Promise<SoloSpaceDetail> {
  if (USE_MOCK) {
    const found = MOCK_SOLO_SPACES[id];
    if (found) {
      return mockResponse({
        ...found,
        totalCount: found.checklist.length,
        doneCount: found.checklist.filter((item) => item.state === 'done').length,
      });
    }
    // 알 수 없는 id 는 빈 목록으로 돌려준다 (다른 항목 내용을 잘못 보여주지 않도록)
    return mockResponse({
      id,
      title: '새 개인 TODO',
      type: '기타',
      createdAt: '방금 생성',
      memo: '',
      totalCount: 0,
      doneCount: 0,
      checklist: [],
    });
  }
  const dto = await http.get<PersonalTodoDetailResponse>(`/todos/${id}`);
  // 상세에는 첫 페이지만 실려 온다. 나머지가 있으면 항목 API 로 전부 받아야 화면에서 체크할 수 있다
  const items = dto.hasMoreItems ? await fetchRemainingItems(id) : dto.items;
  return {
    id: String(dto.id),
    title: dto.title,
    type: todoCategoryLabel(dto.category),
    createdAt: toDateText(dto.createDate),
    memo: dto.memo ?? '',
    totalCount: dto.totalCount,
    doneCount: dto.doneCount,
    checklist: items.map(toChecklistItem),
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
    });
  }
  return toChecklistItem(
    await http.post<PersonalTodoItemResponse>(`/todos/${id}/items`, { content }),
  );
}

/** PATCH /todos/{id}/items/{itemId} — 할 일 내용 수정. 완료 여부는 완료 API가 따로 다룬다. */
export async function updateSoloItem(
  id: string,
  itemId: string,
  content: string,
): Promise<ChecklistItem> {
  if (USE_MOCK) {
    return mockResponse({
      id: itemId,
      content,
      state: 'open',
    });
  }
  return toChecklistItem(
    await http.patch<PersonalTodoItemResponse>(`/todos/${id}/items/${itemId}`, { content }),
  );
}

/** DELETE /todos/{id}/items/{itemId} — 할 일 삭제. */
export async function deleteSoloItem(id: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.delete<void>(`/todos/${id}/items/${itemId}`);
}

/**
 * POST /todos/{id}/items/{itemId}/complete — 할 일 완료.
 * 서버에 되돌리기 API 가 없어 완료만 있다(화면에도 되돌리기 UI 가 없다).
 */
export async function completeSoloItem(id: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<void>(`/todos/${id}/items/${itemId}/complete`);
}

/** PATCH /todos/{id} — TODO 완료 상태 변경 */
export async function finishTodo(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch(`/todos/${id}`, { status: 'ACHIEVED' });
}
