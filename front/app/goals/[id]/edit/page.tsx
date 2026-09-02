import Link from 'next/link';
import { notFound } from 'next/navigation';
import { GoalEditForm } from '@/components/goal/GoalEditForm';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';
import { ApiError, USE_MOCK, fetchGoalDetail, fetchMyProfileOrNull } from '@/lib/api';
import { MOCK_GOAL_VIEWER_ID } from '@/lib/mock';

/** 수정할 수 없는 이유. 이유마다 안내 문구가 달라 갈라서 돌려준다 */
type Blocked = 'unauthorized' | 'notOwner' | 'platformVerified';

const BLOCKED_MESSAGES: Record<Blocked, { title: string; description: string }> = {
  unauthorized: {
    title: '로그인이 필요해요',
    description: '성취 수정은 로그인한 회원만 할 수 있어요.',
  },
  notOwner: {
    title: '수정할 수 없는 성취예요',
    description: '본인이 등록한 성취만 수정할 수 있어요.',
  },
  platformVerified: {
    title: '수정할 수 없는 성취예요',
    description:
      '파티 활동에 따라 플랫폼이 자동으로 남긴 기록이라 직접 고칠 수 없어요. 파티 내용이 바뀌면 함께 반영됩니다.',
  },
};

export default async function GoalEditPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  let goal;
  try {
    goal = await fetchGoalDetail(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    if (error instanceof ApiError && error.status === 401) return <Blocker reason="unauthorized" id={id} />;
    throw error;
  }

  // 목업 회원 id 는 'haneul' 같은 문자열이라 서버의 숫자 id 와 맞지 않는다
  const viewer = await fetchMyProfileOrNull();
  const viewerId = USE_MOCK ? String(MOCK_GOAL_VIEWER_ID) : viewer?.id;

  if (viewerId == null) return <Blocker reason="unauthorized" id={id} />;
  if (viewerId !== String(goal.ownerId)) return <Blocker reason="notOwner" id={id} />;
  // 서버도 자동기록 성취의 수정을 409-1 로 막는다. 폼을 띄우기 전에 걸러낸다
  if (goal.source !== 'SELF_REPORTED') return <Blocker reason="platformVerified" id={id} />;

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '47.5rem' }}>
        <BackLink href={`/goals/${id}`} label="성취로 돌아가기" />

        <SectionHead
          title="성취 수정"
          description="직접 등록한 성취의 내용과 진행 상태를 고칠 수 있어요."
        />

        <GoalEditForm goal={goal} />
      </div>
    </main>
  );
}

function Blocker({ reason, id }: { reason: Blocked; id: string }) {
  const { title, description } = BLOCKED_MESSAGES[reason];

  return (
    <main>
      <div className="board-wrap container" style={{ maxWidth: '40rem' }}>
        <BackLink href={`/goals/${id}`} label="성취로 돌아가기" />
        <SectionHead title={title} description={description} />
        <Link className="btn btn-ghost" href={reason === 'unauthorized' ? '/login' : `/goals/${id}`}>
          {reason === 'unauthorized' ? '로그인하러 가기' : '성취 보기'}
        </Link>
      </div>
    </main>
  );
}
