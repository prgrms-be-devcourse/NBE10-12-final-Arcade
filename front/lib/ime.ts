import type { KeyboardEvent } from 'react';

/**
 * 이 Enter 가 '입력을 마쳤다'는 뜻인지 판별한다.
 *
 * 한글·일본어·중국어는 글자를 조합해 만든다(IME). 조합 중에 누른 Enter 는 **조합 확정**이지
 * 입력 완료가 아닌데, 브라우저는 그때도 keydown 을 보낸다. 그래서 확정용 Enter 와 진짜 Enter 가
 * 연달아 두 번 들어오고, 조합 확정 시점에 이미 값을 읽어 입력칸을 비워 버리면
 * 남은 조각이 두 번째 Enter 에 다시 등록된다 — '스킬' 을 넣었는데 '스킬' 과 '킬' 이 함께
 * 태그로 잡히던 증상이 이것이다.
 *
 * isComposing 은 React 합성 이벤트에 없어 nativeEvent 에서 읽는다.
 * keyCode 229 는 isComposing 을 채우지 않는 브라우저(구형 Safari 등)를 위한 같은 뜻의 신호다.
 *
 * 텍스트 입력에만 쓴다 — 버튼 역할을 하는 요소의 Enter 는 조합과 무관하다.
 */
export function isEnterCommit(event: KeyboardEvent): boolean {
  return (
    event.key === 'Enter' && !event.nativeEvent.isComposing && event.nativeEvent.keyCode !== 229
  );
}
