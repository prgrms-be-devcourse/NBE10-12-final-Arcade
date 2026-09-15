import type { SVGProps } from 'react';

export type IconName =
  | 'i-joystick'
  | 'i-bell'
  | 'i-users'
  | 'i-crown'
  | 'i-trophy'
  | 'i-check'
  | 'i-external'
  | 'i-plus'
  | 'i-mail'
  | 'i-clock'
  | 'i-x'
  | 'i-heart'
  | 'i-bookmark'
  | 'i-eye'
  | 'i-search'
  | 'i-comment'
  | 'i-trash'
  | 'i-pencil'
  | 'i-chevron-left'
  | 'i-chevron-right'
  | 'i-moon'
  | 'i-sun'
  | 'i-google'
  | 'i-kakao'
  | 'i-github'
  | 'i-logout';

interface IconProps extends SVGProps<SVGSVGElement> {
  name: IconName;
}

/** IconSprite 에 정의된 심볼을 참조하는 공용 아이콘 */
export function Icon({ name, viewBox = '0 0 24 24', ...rest }: IconProps) {
  return (
    <svg viewBox={viewBox} {...rest}>
      <use href={`#${name}`} />
    </svg>
  );
}
