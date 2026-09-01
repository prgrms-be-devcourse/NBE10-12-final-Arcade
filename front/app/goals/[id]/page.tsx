import Link from 'next/link';
import { notFound } from 'next/navigation';
import { GoalDetailView } from '@/components/goal/GoalDetailView';
import { BackLink } from '@/components/ui/BackLink';
import { SectionHead } from '@/components/ui/SectionHead';
import { ApiError, USE_MOCK, fetchGoalDetail, fetchMyProfileOrNull } from '@/lib/api';
import { MOCK_GOAL_VIEWER_ID } from '@/lib/mock';
import type { GoalDetailResponse } from '@/lib/api';

/**
 * 없는 성취(404)와 미로그인(401)을 구분해서 돌려준다.
 * notFound() 는 렌더 경로에서 던져야 해서 여기서 부르지 않고 결과만 넘긴다.
 */
async function loadGoal(
  id: string,
): Promise<{ goal: GoalDetailResponse } | { goal: null; reason: 'missing' | 'unauthorized' }> {
  try {
    return { goal: await fetchGoalDetail(id) };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return { goal: null, reason: 'missing' };
    if (error instanceof ApiError && error.status === 401) {
      return { goal: null, reason: 'unauthorized' };
    }
    throw error;
  }
}

export default async function GoalDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const loaded = await loadGoal(id);

  if (loaded.goal === null) {
    if (loaded.reason === 'missing') notFound();

    // 기획서 9.4 는 비인증 조회지만 서버 인가 규칙상 아직 로그인이 필요하다.
    // 로그인 화면으로 밀어내지 않고 안내만 하고, 돌아갈 길을 남긴다.
    return (
      <main>
        <div className="board-wrap container" style={{ maxWidth: '40rem' }}>
          <BackLink href="/" label="메인으로" />
          <SectionHead
            title="로그인이 필요해요"
            description="성취 상세는 로그인한 회원에게만 열려 있어요."
          />
          <Link className="btn primary" href="/login">
            로그인하러 가기
          </Link>
        </div>
      </main>
    );
  }

  // 성취는 전체 공개라 남의 것도 볼 수 있다.
  // 본인 여부는 팀 스페이스 버튼과 PR 목록이 비는 이유를 가르는 데만 쓴다.
  const viewer = await fetchMyProfileOrNull();

  // 목업 회원 id 는 'haneul' 같은 문자열이라 서버의 숫자 id 와 맞지 않는다
  const viewerId = USE_MOCK ? String(MOCK_GOAL_VIEWER_ID) : viewer?.id;

  return <GoalDetailView goal={loaded.goal} viewerId={viewerId} />;
}
