import { SoloSpaceLoader } from '@/components/mypage/SoloSpaceLoader';
import { BackLink } from '@/components/ui/BackLink';
import { MOCK_CURRENT_USER_ID, MOCK_USER_SUMMARIES } from '@/lib/mock';

export default async function SoloTodoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/mypage?tab=todo" label="개인 TODO 목록으로" />
        <SoloSpaceLoader id={id} ownerName={MOCK_USER_SUMMARIES[MOCK_CURRENT_USER_ID].name} />
      </div>
    </main>
  );
}
