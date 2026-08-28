import Link from 'next/link';
import { AdminConsole } from '@/components/admin/AdminConsole';
import {
  fetchAdminAwards,
  fetchAdminBadges,
  fetchAdminHosts,
  fetchAdminMembers,
  fetchAdminParties,
  fetchAdminReports,
  fetchAdminStats,
} from '@/lib/api';
import {
  MOCK_ADMIN_AWARDS,
  MOCK_ADMIN_BADGES,
  MOCK_ADMIN_KPIS,
  MOCK_ADMIN_MEMBERS,
  MOCK_ADMIN_PARTIES,
  MOCK_ADMIN_SIGNUP_CHART,
  MOCK_ADMIN_SIGNUP_TABLE,
} from '@/lib/mock';

export default async function AdminPage() {
  const [stats, parties, members, hosts, badges, reports, awards] = await Promise.all([
    fetchAdminStats() as Promise<{
      kpis: typeof MOCK_ADMIN_KPIS;
      chart: typeof MOCK_ADMIN_SIGNUP_CHART;
      table: typeof MOCK_ADMIN_SIGNUP_TABLE;
    }>,
    fetchAdminParties() as Promise<typeof MOCK_ADMIN_PARTIES>,
    fetchAdminMembers() as Promise<typeof MOCK_ADMIN_MEMBERS>,
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
            <p>가입 통계부터 신고 처리까지, 운영에 필요한 데이터를 한 곳에서 관리해요.</p>
          </div>
          <Link className="btn btn-ghost" href="/">
            서비스로 돌아가기
          </Link>
        </div>

        <AdminConsole
          stats={stats}
          parties={parties}
          members={members}
          hosts={hosts}
          badges={badges}
          reports={reports}
          awards={awards}
        />
      </div>
    </main>
  );
}
