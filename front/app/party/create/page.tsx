import { redirect } from 'next/navigation';
import { PartyCreateForm } from '@/components/party/PartyCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchMyProfileOrNull, fetchParty } from '@/lib/api';

export default async function PartyCreatePage({
  searchParams,
}: {
  searchParams: Promise<{ edit?: string }>;
}) {
  const { edit } = await searchParams;

  /**
   * 수정 진입은 "파티장 본인 + 모집 중" 일 때만 허용한다.
   * 사이드바의 수정 버튼은 이 조건일 때만 보이지만(LeaderTools), 그건 버튼을 숨길 뿐이라
   * URL을 직접 열면 누구든 폼과 기존 데이터를 볼 수 있었다 — 서버 컴포넌트에서 다시 막는다.
   */
  if (edit) {
    const [me, party] = await Promise.all([fetchMyProfileOrNull(), fetchParty(edit).catch(() => null)]);
    if (!party) redirect('/party');
    if (!me || me.id !== party.leader.id || party.status !== 'RECRUITING') {
      redirect(`/party/${edit}`);
    }
  }

  return (
    <main>
      <div className="board-wrap container container--form">
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
