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
          title="파티 모집"
          description="포지션과 마감일을 확인하고, 파티장의 프로필로 신뢰도를 판단한 뒤 지원하세요."
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
