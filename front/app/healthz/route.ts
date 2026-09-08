// 컨테이너 헬스체크용. 프론트 자신만 검사한다 (첫 화면은 SSR 이 백엔드를 부른다).

// 빼면 응답이 빌드 시점에 계산돼 캐시된다 — 요청마다 서버 코드가 도는지는 확인 못 한다.
export const dynamic = "force-dynamic";

export function GET() {
  return new Response("ok", {
    status: 200,
    headers: {
      "content-type": "text/plain; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}
