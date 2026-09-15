import { SoloSpaceLoader } from '@/components/mypage/SoloSpaceLoader';
import { BackLink } from '@/components/ui/BackLink';

export default async function SoloTodoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/mypage?tab=todo" label="개인 TODO 목록으로" />
        <SoloSpaceLoader id={id} />
      </div>
    </main>
  );
}
