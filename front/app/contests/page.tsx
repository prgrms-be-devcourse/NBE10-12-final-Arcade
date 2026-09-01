import { ContestBoard } from '@/components/contest/ContestBoard';
import { LinkButton } from '@/components/ui/Button';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchMyProfileOrNull } from '@/lib/api';

export default async function ContestBoardPage() {
  // 공개 페이지라 비로그인도 볼 수 있어야 한다
  const profile = await fetchMyProfileOrNull();
  // 대회 등록은 주최측(HOST)·관리자(ADMIN)만 할 수 있다 (기획서 2.4)
  const canRegister = profile != null && profile.memberRole !== 'MEMBER';

  return (
    <main>
      <div className="board-wrap container">
        <SectionHead
          title="공모전 · 대회"
          description="주최측이 직접 등록한 대회만 모아, 신뢰할 수 있는 정보로 팀을 구성하세요."
          action={
            canRegister ? <LinkButton href="/contests/create">대회 등록</LinkButton> : undefined
          }
        />
        <ContestBoard />
      </div>
    </main>
  );
}
