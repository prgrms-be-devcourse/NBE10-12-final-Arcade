import type { ChecklistItem, TodoItem } from '@/lib/types';
import { MOCK_SOLO_SPACES, MOCK_TODOS } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

/** 개인 TODO 상세 — 화면(SoloSpace)이 쓰는 모양. 백엔드 PersonalTodoDetailDto 를 옮긴 것. */
export interface SoloSpaceDetail {
  id: string;
  title: string;
  /** 분류 한글 라벨 */
  type: string;
  createdAt: string;
  memo: string;
  checklist: ChecklistItem[];
}

/* ---------- 백엔드 응답 타입 ---------- */

type TodoCategory = 'STUDY' | 'SIDE' | 'CAREER' | 'CERTIFICATE' | 'ETC';

/** PersonalTodoItemDto */
interface PersonalTodoItemResponse {
  id: number;
  content: string;
  done: boolean;
  doneAt: string | null;
  sortOrder: number;
}

/** PersonalTodoDetailDto — items 는 첫 페이지(20건)만 담긴다 */
interface PersonalTodoDetailResponse {
  id: number;
  ownerId: number;
  title: string;
  category: TodoCategory;
  memo: string | null;
  status: string;
  totalCount: number;
  doneCount: number;
  items: PersonalTodoItemResponse[];
  hasMoreItems: boolean;
  createDate: string;
  modifyDate: string;
}

/* ---------- 매퍼 ---------- */

const CATEGORY_LABEL: Record<TodoCategory, string> = {
  STUDY: '학습',
  SIDE: '사이드',
  CAREER: '커리어',
  CERTIFICATE: '자격증',
  ETC: '기타',
};

function toChecklistItem(item: PersonalTodoItemResponse): ChecklistItem {
  return { id: String(item.id), content: item.content, state: item.done ? 'done' : 'open' };
}

function toSoloSpaceDetail(
  dto: PersonalTodoDetailResponse,
  items: PersonalTodoItemResponse[],
): SoloSpaceDetail {
  return {
    id: String(dto.id),
    title: dto.title,
    type: CATEGORY_LABEL[dto.category] ?? dto.category,
    createdAt: dto.createDate.slice(0, 10).replace(/-/g, '.'),
    memo: dto.memo ?? '',
    checklist: items.map(toChecklistItem),
  };
}

/* ---------- 목록 · 생성 ---------- */

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

/* ---------- 상세 ---------- */

const ITEM_PAGE_SIZE = 100;
/** 개인 TODO 항목이 이걸 넘을 일은 없다. 백엔드 페이지네이션이 오작동해도 무한 루프에 빠지지 않도록 상한을 둔다. */
const MAX_ITEM_PAGES = 20;

/** GET /todos/{id}/items — 상세 응답의 items 는 첫 20건뿐이라, 남으면 여기서 끝까지 받는다 */
async function fetchAllTodoItems(id: string): Promise<PersonalTodoItemResponse[]> {
  const all: PersonalTodoItemResponse[] = [];
  for (let page = 0; page < MAX_ITEM_PAGES; page += 1) {
    const res = await http.get<{ content: PersonalTodoItemResponse[]; last?: boolean }>(
      `/todos/${id}/items`,
      { query: { page, size: ITEM_PAGE_SIZE } },
    );
    all.push(...res.content);
    if (res.last === true || res.content.length < ITEM_PAGE_SIZE) return all;
  }
  console.warn(`할 일 항목이 ${MAX_ITEM_PAGES * ITEM_PAGE_SIZE}건을 넘어 일부만 불러왔습니다 (todo ${id})`);
  return all;
}

/** GET /todos/{id} */
export async function fetchSoloSpace(id: string): Promise<SoloSpaceDetail> {
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
  const items = dto.hasMoreItems ? await fetchAllTodoItems(id) : dto.items;
  return toSoloSpaceDetail(dto, items);
}

/** PATCH /todos/{id} — 부분 수정이라 메모만 보낸다 */
export async function saveSoloMemo(id: string, memo: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch(`/todos/${id}`, { memo });
}

/** PATCH /todos/{id} — 완료 처리 */
export async function finishTodo(id: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch(`/todos/${id}`, { status: 'ACHIEVED' });
}

/* ---------- 할 일 항목 ---------- */

/** POST /todos/{id}/items */
export async function createSoloItem(id: string, content: string): Promise<ChecklistItem> {
  if (USE_MOCK) {
    return mockResponse({ id: `solo-${Date.now()}`, content, state: 'open' });
  }
  return toChecklistItem(
    await http.post<PersonalTodoItemResponse>(`/todos/${id}/items`, { content }),
  );
}

/** PATCH /todos/{id}/items/{itemId} — 내용 수정 */
export async function updateSoloItem(
  id: string,
  itemId: string,
  content: string,
): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.patch<PersonalTodoItemResponse>(`/todos/${id}/items/${itemId}`, { content });
}

/** DELETE /todos/{id}/items/{itemId} */
export async function deleteSoloItem(id: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.delete<void>(`/todos/${id}/items/${itemId}`);
}

/** POST /todos/{id}/items/{itemId}/complete */
export async function completeSoloItem(id: string, itemId: string): Promise<void> {
  if (USE_MOCK) return mockResponse(undefined as void);
  await http.post<PersonalTodoItemResponse>(`/todos/${id}/items/${itemId}/complete`);
}
