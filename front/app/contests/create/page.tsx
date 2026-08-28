import { ContestCreateForm } from '@/components/contest/ContestCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';

export default async function ContestCreatePage({
  searchParams,
}: {
  searchParams: Promise<{ edit?: string }>;
}) {
  const { edit } = await searchParams;

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href="/contests" />

        <SectionHead
          title={edit ? '공모전 수정' : '공모전 등록'}
          description="사업자·기관은 물론 개인·동아리·학과도 간단한 승인만으로 등록할 수 있어요."
        />

        {edit ? (
          <div className="form-mode-banner">
            이미 등록된 공모전을 수정하고 있어요. 접수 시작 이후에는 <b>접수 종료일 연장</b>과 소개
            문구만 바꾸는 것을 권장합니다.
          </div>
        ) : (
          <div className="form-banner">
            등록 신청은 PENDING 상태로 접수돼요. 관리자가 간단히 확인하고 승인하면 공모전 허브에
            노출되고, 이후 참가자들이 파티 생성 시 이 공모전을 검색해 연동할 수 있어요.
          </div>
        )}

        <ContestCreateForm editId={edit} />
      </div>
    </main>
  );
}
