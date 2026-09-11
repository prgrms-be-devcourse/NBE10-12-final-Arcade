/**
 * 사용자가 입력한 주소를 링크로 그려도 되는지 판단한다.
 *
 * 대회 링크(`PersonalContest.contestUrl`)처럼 회원이 자유롭게 적는 값은 **서버가 검증하지 않는다.**
 * 등록·수정 화면이 `https?://` 를 확인하지만 그건 1차 안내일 뿐이라 API 를 직접 부르면 그대로 저장된다.
 *
 * 그 값을 그대로 `href` 에 실으면 `javascript:` 같은 스킴이 클릭 시점에 실행될 수 있다.
 * 특히 관리자 검수 화면은 남이 적은 주소를 관리자가 누르는 자리라 위험이 크다.
 *
 * http · https 가 아니면 null 을 돌려주니, 호출부는 링크 대신 평문으로 그리면 된다.
 */
export function httpUrlOrNull(value: string | null | undefined): string | null {
  if (!value) return null;

  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:' ? value : null;
  } catch {
    // 주소로 파싱되지 않는 값(상대 경로·오타)은 링크로 걸지 않는다
    return null;
  }
}
