import Link from 'next/link';
import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'ghost';

function classes(variant: Variant, className?: string) {
  return ['btn', variant === 'primary' ? 'btn-primary' : 'btn-ghost', className]
    .filter(Boolean)
    .join(' ');
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  children: ReactNode;
}

export function Button({ variant = 'primary', className, children, ...rest }: ButtonProps) {
  return (
    <button type="button" className={classes(variant, className)} {...rest}>
      {children}
    </button>
  );
}

interface LinkButtonProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  variant?: Variant;
  children: ReactNode;
}

export function LinkButton({
  href,
  variant = 'primary',
  className,
  children,
  ...rest
}: LinkButtonProps) {
  return (
    <Link href={href} className={classes(variant, className)} {...rest}>
      {children}
    </Link>
  );
}
