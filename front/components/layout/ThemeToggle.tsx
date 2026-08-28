'use client';

import { useCallback, useSyncExternalStore } from 'react';
import { Icon } from '@/components/icons/Icon';

type Theme = 'dark' | 'light';

export const THEME_STORAGE_KEY = 'crewon-theme';
const THEME_EVENT = 'crewon:theme-change';

/**
 * 테마는 html[data-theme] 하나만을 진실의 원천으로 삼는다.
 * (초기값은 layout.tsx 의 ThemeScript 가 하이드레이션 전에 세팅한다)
 */
function subscribe(onChange: () => void) {
  window.addEventListener(THEME_EVENT, onChange);
  return () => window.removeEventListener(THEME_EVENT, onChange);
}

function getSnapshot(): Theme {
  return document.documentElement.dataset.theme === 'light' ? 'light' : 'dark';
}

/** 다크/라이트 전환 */
export function ThemeToggle() {
  const theme = useSyncExternalStore(subscribe, getSnapshot, () => 'dark' as Theme);

  const toggle = useCallback(() => {
    const next: Theme = getSnapshot() === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, next);
    } catch {
      /* 저장 실패는 무시 — 화면 전환은 그대로 동작한다 */
    }
    window.dispatchEvent(new Event(THEME_EVENT));
  }, []);

  return (
    <button type="button" className="theme-toggle" onClick={toggle} aria-label="화면 모드 전환">
      <span>
        <Icon name={theme === 'dark' ? 'i-moon' : 'i-sun'} />
      </span>
      <span className="theme-label">{theme === 'dark' ? '다크' : '라이트'}</span>
    </button>
  );
}
