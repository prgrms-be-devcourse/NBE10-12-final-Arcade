import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { Footer } from '@/components/layout/Footer';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LinkButton } from '@/components/ui/Button';
import { StatusPill } from '@/components/ui/Tag';
import {
  fetchHostCompany,
  fetchHostContests,
  fetchHostFiles,
  fetchHostStats,
  fetchHostTeams,
} from '@/lib/api';
import { CONTEST_FORMAT_LABELS } from '@/lib/constants';
import { MOCK_HOST_FILES, MOCK_HOST_STATS, MOCK_HOST_TEAMS } from '@/lib/mock';

export default async function HostMyPage() {
  const [company, stats, contests, teams, files] = await Promise.all([
    fetchHostCompany(),
    fetchHostStats() as Promise<typeof MOCK_HOST_STATS>,
    fetchHostContests(),
    fetchHostTeams() as Promise<typeof MOCK_HOST_TEAMS>,
    fetchHostFiles() as Promise<typeof MOCK_HOST_FILES>,
  ]);

  return (
    <main>
      <div className="mypage-wrap container">
        <section className="profile-card host-card">
          <div className="host-logo">{company.name.charAt(0)}</div>
          <div className="profile-info">
            <div className="profile-name-row">
              <h2>{company.name}</h2>
              {company.verified ? (
                <span className="badge verified">
                  <Icon name="i-check" />
                  사업자 인증
                </span>
              ) : null}
              <StatusPill tone="live">주최측 계정</StatusPill>
            </div>
            <p className="profile-role">기업 · 사업자등록번호 {company.bizNumber}</p>
            <p className="profile-bio">{company.intro}</p>
            <div className="hero-meta-row">
              <span className="hero-meta-item">
                담당자 <b>{company.manager}</b>
              </span>
              <span className="hero-meta-item">
                가입 <b>2025.11</b>
              </span>
            </div>
          </div>
        </section>

        <div className="host-stat-row">
          {stats.map((stat) => (
            <div key={stat.label} className="host-stat">
              <p className="k">{stat.label}</p>
              <p className="v">{stat.value}</p>
            </div>
          ))}
        </div>

        <div className="mypage-grid" style={{ marginTop: '1.625rem' }}>
          <DetailGrid
            main={
              <>
                <Block>
                  <div className="board-head-row" style={{ marginBottom: '0.875rem' }}>
                    <h3 className="block-title" style={{ marginBottom: 0 }}>
                      등록한 공모전
                    </h3>
                    <LinkButton
                      href="/contests/create"
                      className="host-contest-add"
                    >
                      공모전 등록
                    </LinkButton>
                  </div>

                  {contests.map((contest) => (
                    <div key={contest.id} className="host-contest-row">
                      <div className="hc-poster">{contest.tag}</div>
                      <div className="hc-body">
                        <div className="hc-top">
                          <h4>
                            {contest.title}{' '}
                            <span className="tag">{CONTEST_FORMAT_LABELS[contest.format]}</span>
                          </h4>
                          <StatusPill tone={contest.status === '접수중' ? 'live' : 'default'}>
                            {contest.status}
                          </StatusPill>
                        </div>
                        <p className="hc-sub">
                          접수 {contest.period} · {contest.dday}
                        </p>
                        <div className="hc-metrics">
                          <span className="view-count">
                            <Icon name="i-eye" />
                            {contest.viewCount.toLocaleString()}
                          </span>
                          <span>참가팀 {contest.teams}</span>
                          <span>연동 파티 {contest.linkedParties}</span>
                        </div>
                      </div>
                      <div className="hc-actions">
                        <Link className="btn btn-ghost" href={`/contests/${contest.id}`}>
                          참가팀 보기
                        </Link>
                        <Link className="btn btn-ghost" href={`/contests/create?edit=${contest.id}`}>
                          수정
                        </Link>
                      </div>
                    </div>
                  ))}
                </Block>

                <Block
                  title="참가 팀 현황"
                  description="크루온에서 파티를 만들어 참가한 팀만 표시됩니다. 팀 페이지의 체크리스트 진행률을 함께 볼 수 있어요."
                >
                  <div className="checklist">
                    {teams.map((team) => (
                      <div key={team.name} className="host-team-row">
                        <span className="tname">{team.name}</span>
                        <span className="tsub">
                          {team.contest} · 팀원 {team.members}명
                        </span>
                        <StatusPill tone={team.live ? 'live' : 'default'}>{team.progress}</StatusPill>
                      </div>
                    ))}
                  </div>
                </Block>
              </>
            }
            side={
              <>
                <SideCard title="회사 정보">
                  <p className="leader-stat-line" style={{ marginBottom: '0.625rem' }}>
                    기업 · 담당자 {company.manager}
                  </p>
                  <a
                    className="link-row"
                    href={`https://${company.homepage}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <span className="icon">
                      <Icon name="i-external" />
                    </span>
                    <span className="txt">
                      <span className="k">공식 홈페이지</span>
                      <br />
                      <span className="v">{company.homepage}</span>
                    </span>
                  </a>
                  <p style={{ marginTop: '0.625rem', fontSize: '.76rem', color: 'var(--text-dim)', lineHeight: 1.5 }}>
                    사업자등록번호 진위확인 완료 · 2025.11.14
                  </p>
                  <h4 style={{ marginTop: '1.125rem' }}>증빙 서류</h4>
                  <div className="file-list">
                    {files.map((file) => (
                      <div key={file.id} className="file-row">
                        <span className="fname">{file.name}</span>
                        <span className="fsize">
                          {file.size} · {file.uploadedAt}
                        </span>
                      </div>
                    ))}
                  </div>
                </SideCard>

                <SideCard title="담당자">
                  <div className="leader-row leader-card-row">
                    <span className="leader-avatar">{company.manager.charAt(0)}</span>
                    <span className="leader-info">
                      <span className="name">{company.manager.split(' · ')[0]}</span>
                      <span className="role">{company.manager.split(' · ')[1] ?? '담당자'}</span>
                    </span>
                  </div>
                  <p className="leader-stat-line">쪽지 3건 · 미확인 1건</p>
                </SideCard>
              </>
            }
          />
        </div>

        <Footer contained={false} />
      </div>
    </main>
  );
}
