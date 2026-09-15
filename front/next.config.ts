import type { NextConfig } from "next";

// 로컬 스토리지(custom.storage.type=local) 가 반환하는 /uploads/{key} 를
// dev 서버에서 백엔드로 프록시. Caddy 를 안 거치고 localhost:3000 으로 붙을 때 필요하다.
const backendOrigin = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "")
  .replace(/\/api\/v\d+\/?$/, "")
  || "http://localhost:8080";

const nextConfig: NextConfig = {
  // 컨테이너 배포용 산출물.
  // 네이티브 모듈을 쓰는 의존성이 들어오면 outputFileTracingIncludes 를 검토한다.
  output: "standalone",
  async rewrites() {
    return [{ source: "/uploads/:path*", destination: `${backendOrigin}/uploads/:path*` }];
  },
};

export default nextConfig;
