import type { Metadata } from 'next';
import { Gothic_A1, Press_Start_2P } from 'next/font/google';
import { AppShell } from '@/components/layout/AppShell';
import { IconSprite } from '@/components/icons/IconSprite';
import { ThemeScript } from '@/components/layout/ThemeScript';
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
      // ThemeScript 가 하이드레이션 전에 data-theme 을 바꾸므로, DOM 값을 그대로 인정한다
      suppressHydrationWarning
    >
      <head>
        <ThemeScript />
      </head>
      <body>
        <IconSprite />
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
