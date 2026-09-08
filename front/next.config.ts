import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 컨테이너 배포용 산출물.
  // 네이티브 모듈을 쓰는 의존성이 들어오면 outputFileTracingIncludes 를 검토한다.
  output: "standalone",
};

export default nextConfig;
