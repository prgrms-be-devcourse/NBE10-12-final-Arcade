import { redirect } from 'next/navigation';
import { MypageView } from '@/components/mypage/MypageView';
import { isMypageTabKey, type MypageTabKey } from '@/lib/mypageTabs';
import {
  fetchMessages,
  fetchMyApplications,
  fetchMyBookmarks,
  fetchMyPartyApplicants,
  fetchMyProfileOrNull,
  fetchTodos,
} from '@/lib/api';

/** 내가 파티장인 파티 — 관리 탭 필터용 */
const MY_PARTIES = [
  { id: 'oakroom', title: '프로그래머스 오락실 공모전 참여하실분' },
  { id: 'sidefe', title: '사이드프로젝트 프론트·UI/UX 구합니다' },
];

export default async function MyPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>;
}) {
  const { tab } = await searchParams;
  const activeTab: MypageTabKey = isMypageTabKey(tab) ? tab : 'identity';

  const [profile, todos, applicants, myApplications, messages, bookmarks] = await Promise.all([
    fetchMyProfileOrNull(),
    fetchTodos(),
    fetchMyPartyApplicants(),
    fetchMyApplications(),
    fetchMessages(),
    fetchMyBookmarks(),
  ]);

  // 로그인해야 볼 수 있는 화면이다
  if (!profile) redirect('/login');

  return (
    <MypageView
      initialTab={activeTab}
      profile={profile}
      todos={todos}
      applicants={applicants}
      myApplications={myApplications}
      messages={messages}
      bookmarks={bookmarks}
      myParties={MY_PARTIES}
    />
  );
}
