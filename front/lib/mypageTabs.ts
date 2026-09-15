/**
 * 마이페이지 탭 정의.
 *
 * 서버 컴포넌트(app/mypage/page.tsx)와 클라이언트 컴포넌트가 함께 쓰기 때문에
 * 'use client' 모듈이 아닌 일반 모듈에 둔다.
 * ('use client' 모듈에서 값을 import 하면 서버 쪽에서는 클라이언트 참조 프록시가 온다)
 */
export const MYPAGE_TABS = [
  { key: 'identity', label: '프로필' },
  { key: 'todo', label: '개인 TODO' },
  { key: 'manage', label: '관리' },
  { key: 'messages', label: '쪽지' },
  { key: 'bookmarks', label: '북마크함' },
] as const;

export type MypageTabKey = (typeof MYPAGE_TABS)[number]['key'];

export const MYPAGE_TAB_KEYS: readonly MypageTabKey[] = MYPAGE_TABS.map((tab) => tab.key);

export function isMypageTabKey(value: string | undefined): value is MypageTabKey {
  return value !== undefined && MYPAGE_TAB_KEYS.includes(value as MypageTabKey);
}
