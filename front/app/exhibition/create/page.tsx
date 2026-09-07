import { ExhibitionCreateForm } from '@/components/exhibition/ExhibitionCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';

export default async function ExhibitionCreatePage({
  searchParams,
}: {
  searchParams: Promise<{ partyId?: string }>;
}) {
  // 전시는 파티 단위로 게시한다. 팀 스페이스의 '전시 게시' 버튼이 partyId 를 달고 보낸다.
  const { partyId } = await searchParams;

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href="/exhibition" label="전시관으로" />

        <SectionHead
          title="전시 게시"
          description="완료한 파티를 전시관에 공개해요. 참여 팀원과 GitHub 저장소는 파티 정보에서 따라옵니다."
        />

        <ExhibitionCreateForm partyId={partyId} />
      </div>
    </main>
  );
}
