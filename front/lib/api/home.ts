import type { Contest, ExhibitionProject, HeroSlide, Party } from '@/lib/types';
import { MOCK_CONTESTS, MOCK_EXHIBITIONS, MOCK_HERO_SLIDES, MOCK_TOP_PARTIES } from '@/lib/mock';
import { fetchContests } from './contests';
import { fetchParties } from './parties';
import { USE_MOCK, http, mockResponse } from './client';

/** 홈 배너 전용 API는 아직 없어, 실제 API 모드에서도 데모 슬라이드를 유지한다. */
export async function fetchHeroSlides(): Promise<HeroSlide[]> {
  return mockResponse(MOCK_HERO_SLIDES);
}

/** GET /home/top-parties — 좋아요 수(likeCount) 상위 3건 (기획서 2.1) */
export async function fetchTopParties(): Promise<Party[]> {
  if (USE_MOCK) return mockResponse(MOCK_TOP_PARTIES);
  return (await fetchParties({ sort: 'like', size: 3 })).slice(0, 3);
}

/** GET /home/popular-contests — 좋아요 수 상위 (기획서 2.4) */
export async function fetchPopularContests(): Promise<Contest[]> {
  if (USE_MOCK) {
    return mockResponse([...MOCK_CONTESTS].sort((a, b) => b.likeCount - a.likeCount).slice(0, 4));
  }
  return (await fetchContests({ sort: 'POPULAR', size: 4 })).slice(0, 4);
}

/** GET /home/popular-exhibitions — 이번 주 조회수(viewCount) TOP3 (기획서 2.5) */
export async function fetchPopularExhibitions(): Promise<ExhibitionProject[]> {
  if (USE_MOCK) {
    return mockResponse([...MOCK_EXHIBITIONS].sort((a, b) => b.viewCount - a.viewCount).slice(0, 3));
  }

  const showcases = await http.get<PartyShowcaseResponse[]>('/parties/showcase/top3');
  return showcases.map(toExhibitionProject);
}

/** 현재 공개된 파티 전시 응답. OpenAPI 타입 생성 전까지 이 응답만 로컬 타입으로 둔다. */
interface PartyShowcaseResponse {
  partyId: number;
  partyName: string;
  ownerName: string;
  title: string | null;
  description: string | null;
  viewCount: number;
  likeCount: number;
}

function toExhibitionProject(showcase: PartyShowcaseResponse): ExhibitionProject {
  const title = showcase.title ?? showcase.partyName;
  return {
    id: String(showcase.partyId),
    title,
    summary: showcase.description ?? showcase.partyName,
    partyName: showcase.partyName,
    role: 'BACK',
    category: '기타',
    source: 'PLATFORM_VERIFIED',
    skills: [],
    viewCount: showcase.viewCount,
    likeCount: showcase.likeCount,
    sourcePartyId: String(showcase.partyId),
    leader: {
      id: String(showcase.partyId),
      name: showcase.ownerName,
      initial: showcase.ownerName.charAt(0) || 'C',
      role: '',
    },
    thumbnailLabel: title.slice(0, 2),
  };
}
