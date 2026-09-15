import type { Metadata } from 'next';
import { PasswordResetForm } from '@/components/auth/PasswordResetForm';

export const metadata: Metadata = {
  title: '비밀번호 재설정 · CREWON',
  referrer: 'no-referrer',
};

export default async function PasswordResetPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string | string[] }>;
}) {
  const value = (await searchParams).token;
  const token = typeof value === 'string' ? value : '';
  return <main><PasswordResetForm token={token} /></main>;
}
