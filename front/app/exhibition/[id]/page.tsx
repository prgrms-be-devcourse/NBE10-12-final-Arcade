import { Icon } from '@/components/icons/Icon';
import { CommentSection } from '@/components/exhibition/CommentSection';
import { DetailActions } from '@/components/ui/DetailActions';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LeaderRow } from '@/components/ui/Avatar';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { fetchExhibition, fetchExhibitionCommits } from '@/lib/api';
import { MOCK_CURRENT_USER_ID, MOCK_USER_SUMMARIES } from '@/lib/mock';

export default async function ExhibitionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  /**
   * 이 화면의 project 는 GET /parties/{id}/showcase 응답이라 **id 와 sourcePartyId 가 모두 partyId** 다
   * (toExhibitionDetail 이 둘 다 dto.partyId 로 채운다). 그래서 아래에서 어느 쪽을 넘겨도 같다.
   *
   * 전시관 '목록' 카드는 다르다 - toExhibitionProject 의 id 는 **goal id** 이고 partyId 는
   * sourcePartyId 에 따로 담긴다. 목록 쪽 규칙을 여기 적용하지 말 것.
   */
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

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/exhibition" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <div className="detail-header-right" style={{ marginLeft: 'auto' }}>
                  <span className="detail-views">
                    <Icon name="i-eye" />
                    조회 {project.viewCount.toLocaleString()}
                  </span>
                  {/* 좋아요 · 북마크는 대회 상세와 같은 공용 컴포넌트를 쓴다 */}
                  <DetailActions
                    target="party"
                    // 위 주석대로 project.id 와 같은 값이다. 공용 타입에서 optional 이라 ?? 만 붙여 둔다
                    id={project.sourcePartyId ?? project.id}
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

              <Block title="프로젝트 소개">
                <p className="detail-desc">{project.description}</p>
                <ChipRow>
                  {project.skills.map((skill) => (
                    <SkillChip key={skill}>{skill}</SkillChip>
                  ))}
                </ChipRow>
              </Block>

              {commits.length > 0 ? (
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

              {/*
                화면은 그대로 두고 서버 연동만 아직 하지 않은 상태다.
                댓글 API 가 없어(docs/마이페이지-요약API_백엔드_요청.md ⑦) 목록은 항상 비어 있고,
                작성한 댓글은 새로고침하면 사라진다 - lib/api/exhibitions.ts 의 댓글 함수들이
                목 모드로 고정돼 있기 때문이다. 서버가 생기면 그쪽만 열면 된다.
              */}
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
                <div key={member.id || member.name} className="member-contact-row">
                  {/* 서버 전시 상세는 참여자를 이름 목록으로만 준다 - id 가 없으면 링크를 걸지 않는다 */}
                  <LeaderRow user={member} href={member.id ? `/profile/${member.id}` : undefined} card />
                  {member.id && member.id !== MOCK_CURRENT_USER_ID ? (
                    <SendMessageButton recipient={member} variant="icon" />
                  ) : null}
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
