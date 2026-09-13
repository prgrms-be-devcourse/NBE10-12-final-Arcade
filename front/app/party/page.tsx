import { PartyBoard } from '@/components/party/PartyBoard';
import { LinkButton } from '@/components/ui/Button';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchParties, fetchRecommendedParties } from '@/lib/api';
import { MOCK_RECOMMEND_KEYWORDS } from '@/lib/mock';

export default async function PartyBoardPage() {
  const [parties, recommended] = await Promise.all([
    fetchParties(),
    fetchRecommendedParties(),
  ]);

  return (
    <main>
      <div className="board-wrap container">
        <SectionHead
          title="모집"
          description="모집 중인 파티를 먼저 확인하고, 진행 중인 파티의 활동 기록도 둘러보세요."
          action={<LinkButton href="/party/create">파티 만들기</LinkButton>}
        />
        <PartyBoard
          parties={parties}
          recommended={recommended}
          keywords={MOCK_RECOMMEND_KEYWORDS}
        />
      </div>
    </main>
  );
}
