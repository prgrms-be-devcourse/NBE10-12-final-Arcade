import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { CommitTimeline } from '@/components/team/CommitTimeline';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { FinishPartyButton } from '@/components/team/FinishPartyButton';
import { ProjectCard } from '@/components/exhibition/ProjectCard';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LeaderRow } from '@/components/ui/Avatar';
import { DDay, Tag, TagRow } from '@/components/ui/Tag';
import {
  fetchExhibitions,
  fetchTeamCommits,
  fetchTeamRepository,
  fetchTeamSpace,
} from '@/lib/api';
import { MOCK_CURRENT_USER_ID, MOCK_USER_SUMMARIES } from '@/lib/mock';

export default async function TeamSpacePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [team, repository, commits, exhibitions] = await Promise.all([
    fetchTeamSpace(id),
    fetchTeamRepository(id),
    fetchTeamCommits(id),
    fetchExhibitions(),
  ]);

  const currentUser = MOCK_USER_SUMMARIES[MOCK_CURRENT_USER_ID].name;
  const leader = team.members[0];
  const teamProject = exhibitions.find((project) => project.partyName === team.title);

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/mypage" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <TagRow>
                  <Tag>해커톤</Tag>
                  <Tag>웹 개발</Tag>
                  <Tag accent>매칭 완료</Tag>
                </TagRow>
                <DDay>제출 D-18</DDay>
              </div>

              <h1 className="detail-title">{team.title}</h1>
              <div className="pboard-meta" style={{ marginTop: '0.625rem' }}>
                <Icon name="i-users" />
                팀원 {team.members.length}명 · {team.period}
              </div>

              {team.contestName ? (
                <div className="contest-link-card">
                  <div>
                    <p className="clc-label">연동 공모전</p>
                    <h4>{team.contestName}</h4>
                    <p className="clc-sub">페이브릿지 · 접수 2026.07.20 ~ 08.26</p>
                  </div>
                  <Link className="card-link" href="/contests/fintech-hackathon">
                    공모전 보기 →
                  </Link>
                </div>
              ) : null}

              <Block title="팀원">
                <div className="member-list">
                  {team.members.map((member) => (
                    <div key={member.id} className="member-row">
                      <LeaderRow user={member} href={`/profile/${member.id}`} />
                      <span className="member-row-right">
                        {member.id === leader?.id ? <Tag accent>파티장</Tag> : <Tag>팀원</Tag>}
                        {member.id === MOCK_CURRENT_USER_ID ? null : (
                          <SendMessageButton recipient={member} variant="icon" />
                        )}
                      </span>
                    </div>
                  ))}
                </div>
              </Block>

              <Block
                title="진행 기록 · 커밋"
                description="연결된 GitHub 저장소에 푸시하면 웹훅으로 커밋이 자동 수집돼요. 커밋 작성자는 프로필에 등록한 GitHub 사용자명으로 팀원과 연결됩니다."
              >
                <CommitTimeline
                  partyId={team.partyId}
                  commits={commits}
                  threads={team.threads}
                  currentUser={currentUser}
                  quorum={team.commitQuorum}
                />
                <p className="checklist-note">
                  파티가 완료되면 이 커밋 기록이 작성자·승인자·시각과 함께 참여자 성취 프로필의
                  결과물로 저장됩니다. (저장 범위는 팀 논의 중)
                </p>
              </Block>
            </>
          }
          side={
            <>
              {leader ? (
                <SideCard title="파티장">
                  <LeaderRow
                    user={leader}
                    href={`/profile/${leader.id}`}
                    role="백엔드 개발자 · 자동기록 5건"
                    card
                  />
                  <p className="leader-stat-line">완료 파티 3 · 수상 2 · 자동기록 성취 5</p>
                  {leader.id === MOCK_CURRENT_USER_ID ? null : (
                    <div className="side-card-action">
                      <SendMessageButton recipient={leader} />
                    </div>
                  )}
                </SideCard>
              ) : null}

              <FinishPartyButton partyId={team.partyId} />

              <SideCard title="저장소">
                <a className="link-row" href={`https://${repository}`} target="_blank" rel="noopener noreferrer">
                  <span className="icon">
                    <Icon name="i-external" />
                  </span>
                  <span className="txt">
                    <span className="k">GitHub</span>
                    <br />
                    <span className="v">{repository}</span>
                  </span>
                </a>
                <p style={{ marginTop: '0.625rem', fontSize: '.76rem', color: 'var(--text-dim)', lineHeight: 1.5 }}>
                  전시용 링크로만 사용돼요. 커밋·PR 자동 연동은 아직 지원하지 않아요.
                </p>
              </SideCard>

              {teamProject ? (
                <SideCard title="참여한 프로젝트">
                  <div className="project-grid" style={{ gridTemplateColumns: '1fr' }}>
                    <ProjectCard project={teamProject} showLeader={false} />
                  </div>
                </SideCard>
              ) : null}
            </>
          }
        />
      </div>
    </main>
  );
}
