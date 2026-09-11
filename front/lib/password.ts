/** 백엔드 기본 PasswordPolicy와 동일한 사용자 입력 검증 문구다. 최종 판정은 항상 서버가 수행한다. */
export function passwordPolicyError(password: string): string {
  if (password.length < 8 || password.length > 64) return '비밀번호는 8~64자로 입력해 주세요.';
  if (/\s/.test(password)) return '비밀번호에는 공백을 사용할 수 없어요.';
  if (!/\p{L}/u.test(password) || !/\d/.test(password) || !/[^\p{L}\p{N}\s]/u.test(password)) {
    return '문자, 숫자, 특수문자를 각각 하나 이상 포함해 주세요.';
  }
  return '';
}
