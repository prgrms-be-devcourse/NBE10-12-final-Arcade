import { THEME_STORAGE_KEY } from './ThemeToggle';

/**
 * 하이드레이션 전에 저장된 테마를 html[data-theme] 에 적용해
 * 새로고침 시 화면이 번쩍이는 것을 막는다.
 *
 * 이 스크립트는 브라우저가 HTML 을 파싱하는 동안 동기로 실행돼야 의미가 있다.
 * 반대로 클라이언트 렌더에서 만들어진 script 태그는 실행되지 않으므로,
 * 서버에서만 실행 가능한 type 을 주고 클라이언트에서는 text/plain 으로 낮춰 둔다.
 * (Next 공식 가이드 preventing-flash-before-hydration 의 InlineScript 패턴)
 *
 * type 이 서버·클라이언트에서 달라지는 건 의도한 것이라 suppressHydrationWarning 을 붙인다.
 */
export function ThemeScript() {
  const script = `try{var t=localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)});if(t==='light'||t==='dark'){document.documentElement.dataset.theme=t;}}catch(e){}`;

  return (
    <script
      type={typeof window === 'undefined' ? 'text/javascript' : 'text/plain'}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: script }}
    />
  );
}
