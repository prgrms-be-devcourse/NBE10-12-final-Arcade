import { ExhibitionCreateForm } from '@/components/exhibition/ExhibitionCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';

export default async function ExhibitionCreatePage({
  searchParams,
}: {
  searchParams: Promise<{ edit?: string }>;
}) {
  const { edit } = await searchParams;

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href="/exhibition" label="전시관으로" />

        <SectionHead
          title={edit ? '전시 수정' : '전시 등록'}
          description="완료한 프로젝트를 전시관에 공개해요. 파티에서 완료한 프로젝트는 체크리스트 스냅샷이 자동으로 따라옵니다."
        />

        {edit ? (
          <div className="form-mode-banner">
            이미 공개된 전시를 수정하고 있어요. 자동기록된 체크리스트 스냅샷과 참여 팀원 정보는
            수정할 수 없습니다.
          </div>
        ) : null}

        <ExhibitionCreateForm editId={edit} />
      </div>
    </main>
  );
}
