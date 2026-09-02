/**
 * 목록에 붙는 시각 문구.
 *
 * 알림·쪽지처럼 "언제 왔는지"가 중요한 목록이 같은 문구를 쓰도록 한곳에 둔다.
 * 서버는 LocalDateTime 을 시간대 없이 내려주므로 브라우저 시간대로 그대로 읽는다.
 */
export function timeAgo(value: string): string {
  const then = new Date(value).getTime();
  if (Number.isNaN(then)) return '';

  const MINUTE = 60_000;
  const HOUR = 60 * MINUTE;
  const DAY = 24 * HOUR;
  const diff = Date.now() - then;

  if (diff < MINUTE) return '방금 전';
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}분 전`;
  if (diff < DAY) return `${Math.floor(diff / HOUR)}시간 전`;
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)}일 전`;
  return value.slice(0, 10).replace(/-/g, '.');
}
