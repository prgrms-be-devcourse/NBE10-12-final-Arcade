import Link from 'next/link';
import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'ghost' | 'danger';
type Size = 'sm' | 'md' | 'lg';

function classes(variant: Variant, size: Size, fullWidth: boolean, className?: string) {
  return [
    'btn',
    variant === 'primary' ? 'btn-primary' : variant === 'danger' ? 'is-danger' : 'btn-ghost',
    `btn-${size}`,
    fullWidth ? 'btn-block' : null,
    className,
  ]
    .filter(Boolean)
    .join(' ');
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
  loading?: boolean;
  children: ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button type="button" className={classes(variant, size, fullWidth, className)} disabled={disabled || loading} {...rest}>
      {loading ? <span className="btn-loading" aria-live="polite">처리 중…</span> : children}
    </button>
  );
}

interface LinkButtonProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
  children: ReactNode;
}

export function LinkButton({
  href,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className,
  children,
  ...rest
}: LinkButtonProps) {
  return (
    <Link href={href} className={classes(variant, size, fullWidth, className)} {...rest}>
      {children}
    </Link>
  );
}

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  label: string;
  children: ReactNode;
}

/** 아이콘만 있는 조작은 레이블을 API에서 필수로 받아 접근성을 보장한다. */
export function IconButton({ label, className, children, ...rest }: IconButtonProps) {
  return (
    <button type="button" className={['icon-btn', className].filter(Boolean).join(' ')} aria-label={label} {...rest}>
      {children}
    </button>
  );
}
