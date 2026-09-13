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
          description="함께할 동료를 찾고 있는 파티를 둘러보세요."
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
