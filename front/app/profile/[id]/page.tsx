import { Icon } from '@/components/icons/Icon';
import { HeroStats } from '@/components/mypage/HeroStats';
import { ProfileCard } from '@/components/mypage/ProfileCard';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { ProjectCard } from '@/components/exhibition/ProjectCard';
import { BackLink } from '@/components/ui/BackLink';
import { Block, SideCard } from '@/components/ui/Block';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { fetchExhibitions, fetchMyProfileOrNull, fetchUserProfile } from '@/lib/api';

export default async function PublicProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [profile, exhibitions, me] = await Promise.all([
    fetchUserProfile(id),
    fetchExhibitions(),
    fetchMyProfileOrNull(),
  ]);

  // 없는 회원(404)이다. 데모 데이터로 채우면 남의 프로필 자리에 남의 것이 아닌 값이 뜬다.
  if (!profile) {
    return (
      <main>
        <div className="profile-view-wrap">
          <BackLink />
          <p className="notif-empty">찾을 수 없는 회원이에요. 주소를 다시 확인해 주세요.</p>
        </div>
      </main>
    );
  }

  const isMe = profile.id === me?.id;
  const awards = profile.achievements.filter((item) => item.type === 'CONTEST');

  // 참여한 프로젝트는 전시 목록이 아니라 **성취**에서 고른다.
  // 전시 목록에는 소유자 id 가 없어(ownerName 뿐) 예전의 leader.id 비교는 늘 빈 배열이었고,
  // 파티장 기준이라 맞았더라도 참여자는 빠졌다. PROJECT 성취는 확정된 파티원 전원에게 생긴다.
  // exhibited 가 true 인 것만 - 전시글이 게시돼야 전시관 카드가 존재한다.
  const exhibitedPartyIds = new Set(
    profile.achievements
      .filter((item) => item.type === 'PROJECT' && item.exhibited && item.sourcePartyId)
      .map((item) => item.sourcePartyId),
  );
  const myProjects = exhibitions.filter(
    (project) => project.sourcePartyId && exhibitedPartyIds.has(project.sourcePartyId),
  );

  return (
    <main>
      <div className="profile-view-wrap">
        <BackLink />

        <ProfileCard profile={profile} />

        {isMe ? null : (
          <div className="profile-contact">
            <SendMessageButton recipient={profile} />
            <p className="form-hint" style={{ marginTop: 0 }}>
              전시된 결과물이나 성취를 보고 바로 연락할 수 있어요. 답장은 쪽지함으로 옵니다.
            </p>
          </div>
        )}

        <HeroStats streakDays={profile.streakDays} activityHeatmap={profile.activityHeatmap} />

        <Block>
          <div className="history-panel">
            <h4>경력 · 수상</h4>
            {profile.careers.map((career) => (
              <div key={career.id} className="timeline-item">
                <p className="role-line">
                  {career.org} · {career.title}
                </p>
                <p className="period">{career.period}</p>
              </div>
            ))}
            {awards.map((award) => (
              <div key={award.id} className="award-item">
                <span className="icon">
                  <Icon name="i-trophy" />
                </span>
                <div>
                  <h5>
                    {award.title} · {award.year}
                  </h5>
                </div>
              </div>
            ))}
          </div>
        </Block>

        <Block title="스킬">
          <ChipRow>
            {profile.skills.map((skill) => (
              <SkillChip key={skill}>{skill}</SkillChip>
            ))}
          </ChipRow>
        </Block>

        <Block title="참여한 프로젝트">
          <div className="project-grid">
            {myProjects.map((project) => (
              <ProjectCard key={project.id} project={project} showLeader={false} />
            ))}
          </div>
          {myProjects.length === 0 ? <p className="notif-empty">아직 공개된 전시가 없어요.</p> : null}
        </Block>

        <Block>
          <SideCard title="기타 주소">
            {profile.links.map((link) => (
              <a
                key={link.id}
                className="link-row"
                href={link.url.startsWith('http') ? link.url : `https://${link.url}`}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span className="icon">
                  <Icon name="i-external" />
                </span>
                <span className="txt">
                  <span className="k">{link.label}</span>
                  <br />
                  <span className="v">{link.url}</span>
                </span>
              </a>
            ))}
          </SideCard>
        </Block>
      </div>
    </main>
  );
}
