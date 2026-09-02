import { redirect } from 'next/navigation';
import { GoalCreateForm } from '@/components/goal/GoalCreateForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchMyProfileOrNull } from '@/lib/api';

/**
 * 성취 자기신고 등록 화면 (POST /api/v1/goals).
 *
 * 본인 성취를 남기는 화면이라 로그인이 필요하다.
 * 마이페이지와 같은 방식으로, 로그인 화면으로 밀어내지 않고 메인으로 돌려보낸다.
 */
export default async function GoalCreatePage() {
  const profile = await fetchMyProfileOrNull();
  if (!profile) redirect('/');

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href="/mypage" label="마이페이지로" />

        <SectionHead
          title="성취 등록"
          description="대회 수상이나 스스로 정한 목표처럼, 크루온 밖에서 이룬 일을 직접 남기는 자리예요."
        />

        <div className="form-banner">
          여기서 등록한 성취는 <b>자기신고</b>로 기록돼요. 파티 활동으로 남는 프로젝트 성취는 파티가
          확정될 때 자동으로 쌓이니 따로 등록하지 않아도 됩니다.
        </div>

        <GoalCreateForm />
      </div>
    </main>
  );
}
