import { THEME_STORAGE_KEY } from './ThemeToggle';

/**
 * 하이드레이션 전에 저장된 테마를 html[data-theme] 에 적용해
 * 새로고침 시 화면이 번쩍이는 것을 막는다.
 */
export function ThemeScript() {
  const script = `try{var t=localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)});if(t==='light'||t==='dark'){document.documentElement.dataset.theme=t;}}catch(e){}`;
  return <script dangerouslySetInnerHTML={{ __html: script }} />;
}
