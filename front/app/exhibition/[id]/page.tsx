import { Icon } from '@/components/icons/Icon';
import { CommentSection } from '@/components/exhibition/CommentSection';
import { ExhibitionActions } from '@/components/exhibition/ExhibitionActions';
import { DetailActions } from '@/components/ui/DetailActions';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LeaderRow } from '@/components/ui/Avatar';
import { ChipRow, SkillChip, Tag, TagRow } from '@/components/ui/Tag';
import { fetchExhibition, fetchExhibitionCommits } from '@/lib/api';
import { GOAL_SOURCE_LABELS } from '@/lib/constants';
import { MOCK_CURRENT_USER_ID, MOCK_USER_SUMMARIES } from '@/lib/mock';

export default async function ExhibitionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const [project, commits] = await Promise.all([
    fetchExhibition(id),
    fetchExhibitionCommits(id) as Promise<
      {
        sha: string;
        message: string;
        authorName: string;
        authorInitial: string;
        date: string;
        approvers: string[];
      }[]
    >,
  ]);
  const currentUser = MOCK_USER_SUMMARIES[MOCK_CURRENT_USER_ID];
  const githubUrl = project.links.find((link) => link.label === 'GitHub')?.url;

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/exhibition" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <TagRow>
                  {/* 대회 상세와 같은 태그 칩을 쓴다. 플랫폼 자동기록만 강조색 */}
                  <Tag accent={project.source === 'PLATFORM_VERIFIED'}>
                    {GOAL_SOURCE_LABELS[project.source]}
                  </Tag>
                </TagRow>
                <div className="detail-header-right">
                  <span className="detail-views">
                    <Icon name="i-eye" />
                    조회 {project.viewCount.toLocaleString()}
                  </span>
                  {/* 좋아요 · 북마크는 대회 상세와 같은 공용 컴포넌트를 쓴다 */}
                  <DetailActions
                    target="exhibition"
                    id={project.id}
                    likeCount={project.likeCount}
                    likedByMe={project.likedByMe}
                    bookmarkedByMe={project.bookmarkedByMe}
                  />
                </div>
              </div>

              <h1 className="detail-title">{project.title}</h1>
              <p className="pboard-meta" style={{ marginTop: '0.625rem' }}>
                {project.summary}
              </p>

              <div
                className={project.coverImageUrl ? 'exh-hero-thumb has-cover' : 'exh-hero-thumb'}
                style={
                  project.coverImageUrl
                    ? { backgroundImage: `url(${project.coverImageUrl})` }
                    : undefined
                }
              />

              <ExhibitionActions
                exhibitionId={project.id}
                exhibitionTitle={project.title}
                owner={project.leader}
                githubUrl={githubUrl}
              />

              <Block title="프로젝트 소개">
                <p className="detail-desc">{project.description}</p>
                <ChipRow>
                  {project.skills.map((skill) => (
                    <SkillChip key={skill}>{skill}</SkillChip>
                  ))}
                </ChipRow>
              </Block>

              {project.source === 'PLATFORM_VERIFIED' && commits.length > 0 ? (
                <Block
                  title="진행 기록 · 커밋"
                  description="완료 시점의 커밋 내역이 작성자·동료 승인자와 함께 스냅샷으로 남아, 결과뿐 아니라 과정도 보여줘요."
                >
                  <div className="snapshot-list">
                    {commits.map((commit) => (
                      <div key={commit.sha} className="snapshot-item">
                        <span className="commit-sha">{commit.sha}</span>
                        <div className="snapshot-body">
                          <p className="content">{commit.message}</p>
                          <p className="meta">
                            <span className="mini-avatar">{commit.authorInitial}</span>
                            {commit.authorName} · 승인 {commit.approvers.join(', ')} · {commit.date}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </Block>
              ) : null}

              <CommentSection
                exhibitionId={project.id}
                comments={project.comments}
                currentUserName={currentUser.name}
              />
            </>
          }
          side={
            <SideCard title="참여 팀원">
              {project.members.map((member) => (
                <div key={member.id} className="member-contact-row">
                  <LeaderRow user={member} href={`/profile/${member.id}`} card />
                  {member.id === MOCK_CURRENT_USER_ID ? null : (
                    <SendMessageButton recipient={member} variant="icon" />
                  )}
                </div>
              ))}
              <p className="leader-stat-line">진행 기간 {project.period}</p>
            </SideCard>
          }
        />
      </div>
    </main>
  );
}
