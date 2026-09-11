import { redirect } from 'next/navigation';
import { AdminConsole } from '@/components/admin/AdminConsole';
import {
  fetchAdminAwards,
  fetchAdminBadges,
  fetchAdminHosts,
  fetchAdminReports,
  fetchAdminStats,
  fetchMyProfileOrNull,
} from '@/lib/api';
import {
  MOCK_ADMIN_AWARDS,
  MOCK_ADMIN_BADGES,
  MOCK_ADMIN_KPIS,
  MOCK_ADMIN_SIGNUP_CHART,
  MOCK_ADMIN_SIGNUP_TABLE,
} from '@/lib/mock';

/**
 * 관리자 콘솔.
 *
 * 파티 · 회원 · 성취 증빙은 관리자 API 가 있어 콘솔이 직접(클라이언트에서) 페이지 단위로 읽는다.
 * 아래에서 받아오는 것들은 아직 서버가 없어 탭을 감춘 화면의 데모 데이터다 - 서버가 생기면
 * AdminConsole 의 ADMIN_TABS 에서 hidden 만 지우면 된다.
 */
export default async function AdminPage() {
  /*
   * 관리자만 들어온다.
   *
   * 서버가 adm 경로를 ROLE_ADMIN 으로 막고 있어 데이터가 새지는 않지만,
   * 막지 않으면 일반 회원에게 표마다 403 오류가 찍힌 빈 콘솔이 열린다.
   * 화면을 그리기 전에 돌려보낸다.
   */
  const me = await fetchMyProfileOrNull();

  if (!me) redirect('/login');
  if (me.memberRole !== 'ADMIN') redirect('/');

  const [stats, hosts, badges, reports, awards] = await Promise.all([
    fetchAdminStats() as Promise<{
      kpis: typeof MOCK_ADMIN_KPIS;
      chart: typeof MOCK_ADMIN_SIGNUP_CHART;
      table: typeof MOCK_ADMIN_SIGNUP_TABLE;
    }>,
    fetchAdminHosts(),
    fetchAdminBadges() as Promise<typeof MOCK_ADMIN_BADGES>,
    fetchAdminReports(),
    fetchAdminAwards() as Promise<typeof MOCK_ADMIN_AWARDS>,
  ]);

  return (
    <main>
      <div className="admin-wrap container">
        <div className="admin-head">
          <div>
            <span className="admin-badge">ADMIN CONSOLE</span>
            <h1 style={{ marginTop: '0.75rem' }}>크루온 관리자</h1>
            <p>파티 · 회원 · 성취 증빙을 한 곳에서 관리해요.</p>
          </div>
        </div>

        <AdminConsole stats={stats} hosts={hosts} badges={badges} reports={reports} awards={awards} />
      </div>
    </main>
  );
}
