import { redirect } from 'next/navigation';
import { MypageView } from '@/components/mypage/MypageView';
import { isMypageTabKey, type MypageTabKey } from '@/lib/mypageTabs';
import {
  fetchMessagesOrEmpty,
  fetchMyApplications,
  fetchMyBookmarks,
  fetchMyGoalsOrEmpty,
  fetchMyPartyApplicants,
  fetchMyProfileOrNull,
  fetchMySummaryOrEmpty,
  fetchPublishedPartyIds,
  fetchTodos,
} from '@/lib/api';
import type { Applicant } from '@/lib/types';

/**
 * 관리 탭의 파티 필터 목록.
 * 내가 파티장인 파티만 조회하는 API 가 따로 없어, 받은 지원자 목록에 실려 오는
 * (partyId, partyName) 을 중복 없이 모아 쓴다. 지원이 하나도 없는 파티는 목록에 없다.
 */
function toMyParties(applicants: Applicant[]) {
  const byId = new Map(applicants.map((a) => [a.partyId, a.partyName]));
  return Array.from(byId, ([id, title]) => ({ id, title }));
}

export default async function MyPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>;
}) {
  const { tab } = await searchParams;
  const activeTab: MypageTabKey = isMypageTabKey(tab) ? tab : 'identity';

  const [profile, summary, achievements, todos, applicants, myApplications, messages, bookmarks] =
    await Promise.all([
      fetchMyProfileOrNull(),
      // 활동 스코어·스트릭·히트맵은 집계라 프로필과 나뉘어 있다 (GET /members/me/summary)
      fetchMySummaryOrEmpty(),
      // 성취는 프로필 응답에 없고 GET /goals/me 로 따로 온다
      fetchMyGoalsOrEmpty(),
      fetchTodos(),
      fetchMyPartyApplicants(),
      fetchMyApplications(),
      fetchMessagesOrEmpty(),
      fetchMyBookmarks(),
    ]);

  // 전시가 게시된 파티만 히스토리에 '전시 페이지 보기' 를 단다(기획서 2.11).
  // 완료된 파티만 확인한다 - 진행중이면 전시 자체가 없다.
  const publishedPartyIds = await fetchPublishedPartyIds(
    achievements
      .filter((goal) => goal.type === 'PROJECT' && goal.status === 'ACHIEVED')
      .map((goal) => goal.sourcePartyId ?? ''),
  );

  // 로그인해야 볼 수 있는 화면이다
  // 로그인해야 볼 수 있는 화면이지만 로그인 화면으로 밀어내지 않고 메인으로 돌려보낸다.
  // 로그인 없이도 둘러볼 수 있는 서비스라 로그인을 강요하는 인상을 주지 않게 한다.
  if (!profile) redirect('/');

  return (
    <MypageView
      initialTab={activeTab}
      profile={{ ...profile, ...summary, achievements }}
      todos={todos}
      applicants={applicants}
      myApplications={myApplications}
      messages={messages}
      bookmarks={bookmarks}
      myParties={toMyParties(applicants)}
      publishedPartyIds={publishedPartyIds}
    />
  );
}
