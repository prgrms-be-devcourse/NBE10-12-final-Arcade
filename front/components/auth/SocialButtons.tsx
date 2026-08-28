'use client';

import { Icon } from '@/components/icons/Icon';
import { socialLogin } from '@/lib/api';

/** 이번 스코프의 소셜 로그인은 GitHub 하나만 지원한다 */
const PROVIDERS = [
  { key: 'github', label: 'GitHub', icon: 'i-github', viewBox: '0 0 16 16' },
] as const;

interface SocialButtonsProps {
  /** '계속하기'(로그인) 또는 '시작하기'(가입) */
  suffix: string;
  /** 주최측 계정은 소셜 로그인을 지원하지 않아 버튼 자체를 감춘다 */
  hidden?: boolean;
  onSuccess?: () => void;
}

export function SocialButtons({ suffix, hidden, onSuccess }: SocialButtonsProps) {
  if (hidden) return null;

  return (
    <div className="social-btn-group">
      {PROVIDERS.map((provider) => (
        <button
          key={provider.key}
          type="button"
          className={`social-btn social-btn--${provider.key}`}
          onClick={async () => {
            await socialLogin(provider.key);
            onSuccess?.();
          }}
        >
          <Icon name={provider.icon} viewBox={provider.viewBox} />
          {provider.label}로 {suffix}
        </button>
      ))}
    </div>
  );
}
