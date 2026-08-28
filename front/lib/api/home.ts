import type { Contest, ExhibitionProject, HeroSlide, Party } from '@/lib/types';
import { MOCK_CONTESTS, MOCK_EXHIBITIONS, MOCK_HERO_SLIDES, MOCK_TOP_PARTIES } from '@/lib/mock';
import { USE_MOCK, http, mockResponse } from './client';

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
