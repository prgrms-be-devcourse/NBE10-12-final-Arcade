import Link from 'next/link';
import { HeroSlider } from '@/components/home/HeroSlider';
import { ContestCard } from '@/components/contest/ContestCard';
import { ProjectCard } from '@/components/exhibition/ProjectCard';
import { RankCard } from '@/components/party/RankCard';
import { SectionHead } from '@/components/ui/SectionHead';
import {
  fetchHeroSlides,
  fetchPopularContests,
  fetchPopularExhibitions,
  fetchTopParties,
} from '@/lib/api';

export default async function HomePage() {
  const [slides, topParties, contests, exhibitions] = await Promise.all([
    fetchHeroSlides(),
    fetchTopParties(),
    fetchPopularContests(),
    fetchPopularExhibitions(),
  ]);

  return (
    <main>
      <HeroSlider slides={slides} />

      <section className="section top3 container" data-reveal>
        <SectionHead
          title="지금 가장 핫한 파티"
          description="지원자가 몰리는 파티 TOP3를 확인하고, 마감 전에 지원하세요."
        />
        <div className="podium">
          {topParties.slice(0, 3).map((party, index) => (
            <RankCard key={party.id} party={party} rank={(index + 1) as 1 | 2 | 3} />
          ))}
        </div>
      </section>

      <section className="section contests container" data-reveal>
        <SectionHead
          title="인기 공모전 · 대회"
          description="지금 팀을 모집 중인 공모전을 둘러보세요."
        />
        <div className="contest-row">
          {contests.map((contest) => (
            <ContestCard key={contest.id} contest={contest} />
          ))}
        </div>
      </section>

      <section className="section exhibition-top container" data-reveal>
        <SectionHead
          title="인기 전시회"
          description="이번 주 가장 많이 본 완료 프로젝트 TOP 3이에요."
        />
        <div className="exh-top-row">
          {exhibitions.map((project, index) => (
            <ProjectCard key={project.id} project={project} rank={index + 1} />
          ))}
        </div>
        <div style={{ marginTop: '1.625rem', textAlign: 'center' }}>
          <Link className="btn btn-ghost" href="/exhibition">
            전시관 전체 보기
          </Link>
        </div>
      </section>

    </main>
  );
}
