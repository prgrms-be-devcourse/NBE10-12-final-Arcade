import { PartyCreateForm } from '@/components/party/PartyCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';

export default async function PartyCreatePage({
  searchParams,
}: {
  searchParams: Promise<{ edit?: string }>;
}) {
  const { edit } = await searchParams;

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href="/party" />

        <SectionHead
          title={edit ? '파티 수정' : '파티 만들기'}
          description="지역 제한 없이 전국 단위로 모집돼요. 온라인 협업을 전제로 팀원을 구성해 보세요."
        />

        {edit ? (
          <div className="form-mode-banner">
            모집 중인 파티를 수정하고 있어요. 이미 <b>모집이 마감된 포지션</b>은 팀원이 확정돼 있어
            수정할 수 없습니다.
          </div>
        ) : null}

        <PartyCreateForm editId={edit} />
      </div>
    </main>
  );
}
