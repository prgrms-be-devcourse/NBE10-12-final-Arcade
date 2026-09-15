import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';

export function AuthLogo() {
  return (
    <Link href="/" className="auth-logo" aria-label="크루온 홈으로">
      <Icon name="i-joystick" className="logo-mark" />
      <span className="logo-text">
        CREW<span>ON</span>
      </span>
    </Link>
  );
}
