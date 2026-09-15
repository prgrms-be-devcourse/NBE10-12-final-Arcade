'use client';

import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';

interface BackLinkProps {
  /** 지정하면 해당 경로로 이동, 없으면 브라우저 히스토리 뒤로가기 */
  href?: string;
  label?: string;
}

export function BackLink({ href, label = '목록으로' }: BackLinkProps) {
  const router = useRouter();
  return (
    <button
      type="button"
      className="back-link"
      onClick={() => (href ? router.push(href) : router.back())}
    >
      <Icon name="i-chevron-left" width={14} height={14} />
      {label}
    </button>
  );
}
