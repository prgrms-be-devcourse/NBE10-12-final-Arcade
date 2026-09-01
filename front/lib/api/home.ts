import type { Contest, ExhibitionProject, HeroSlide, Party } from '@/lib/types';
import { MOCK_CONTESTS, MOCK_EXHIBITIONS, MOCK_HERO_SLIDES, MOCK_TOP_PARTIES } from '@/lib/mock';
import { http, mockResponse } from './client';

/**
 * 백엔드에 대응 엔드포인트가 아직 없는 모듈이다.
 *
 * 없는 경로로 요청해도 서버 SecurityConfig 의 전체 경로 인증 규칙에 먼저 걸려 404 가 아니라 401 이 오고,
 * 서버 컴포넌트에서 호출한 경우 페이지 전체가 500 으로 죽는다.
 * 그래서 실제 API 가 생기기 전까지는 이 상수로 데모 데이터만 쓰도록 고정한다.
 *
 * 서버가 준비되면 이 상수를 지우고 client 의 USE_MOCK 을 다시 import 하면 아래 http 호출이 살아난다.
 */
const USE_MOCK: boolean = true;


/** GET /home/banners */
export async function fetchHeroSlides(): Promise<HeroSlide[]> {
  if (USE_MOCK) return mockResponse(MOCK_HERO_SLIDES);
  return http.get<HeroSlide[]>('/home/banners');
}

/** GET /home/top-parties — 좋아요 수(likeCount) 상위 3건 (기획서 2.1) */
export async function fetchTopParties(): Promise<Party[]> {
  if (USE_MOCK) return mockResponse(MOCK_TOP_PARTIES);
  return http.get<Party[]>('/home/top-parties');
}

/** GET /home/popular-contests — 좋아요 수 상위 (기획서 2.4) */
export async function fetchPopularContests(): Promise<Contest[]> {
  if (USE_MOCK) {
    return mockResponse([...MOCK_CONTESTS].sort((a, b) => b.likeCount - a.likeCount).slice(0, 4));
  }
  return http.get<Contest[]>('/home/popular-contests');
}

/** GET /home/popular-exhibitions — 이번 주 조회수(viewCount) TOP3 (기획서 2.5) */
export async function fetchPopularExhibitions(): Promise<ExhibitionProject[]> {
  if (USE_MOCK) {
    return mockResponse([...MOCK_EXHIBITIONS].sort((a, b) => b.viewCount - a.viewCount).slice(0, 3));
  }
  return http.get<ExhibitionProject[]>('/home/popular-exhibitions');
}
