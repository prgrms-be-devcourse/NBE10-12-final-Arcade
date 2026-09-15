import type { Metadata } from 'next';
import { Gothic_A1, Press_Start_2P } from 'next/font/google';
import { AppShell } from '@/components/layout/AppShell';
import { IconSprite } from '@/components/icons/IconSprite';
import { THEME_STORAGE_KEY } from '@/lib/constants';
import './globals.css';

const gothicA1 = Gothic_A1({
  variable: '--font-gothic-a1',
  weight: ['400', '500', '600', '700', '800', '900'],
  subsets: ['latin'],
  display: 'swap',
});

const pressStart = Press_Start_2P({
  variable: '--font-press-start',
  weight: '400',
  subsets: ['latin'],
  display: 'swap',
});

export const metadata: Metadata = {
  title: '크루온 오락실 · CREWON',
  description: '완료한 프로젝트와 수상 이력이 자동으로 쌓이는 팀 매칭 플랫폼',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="ko"
      data-theme="dark"
      className={`${gothicA1.variable} ${pressStart.variable}`}
      // 아래 스크립트가 하이드레이션 전에 data-theme 을 바꾸므로, DOM 값을 그대로 인정한다
      suppressHydrationWarning
    >
      <head>
        {/*
          저장된 테마를 브라우저가 HTML 을 파싱하는 동안 동기로 적용해 새로고침 시 번쩍임을 막는다.
          컴포넌트로 감싸면 React 가 "렌더 중에 script 태그를 만났다" 고 경고하므로
          Next 공식 가이드(preventing-flash-before-hydration)대로 head 에 그대로 둔다.
        */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)});if(t==='light'||t==='dark'){document.documentElement.dataset.theme=t}}catch(e){}})()`,
          }}
        />
      </head>
      <body>
        <IconSprite />
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
