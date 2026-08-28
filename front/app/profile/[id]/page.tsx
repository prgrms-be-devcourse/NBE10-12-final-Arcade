import { Icon } from '@/components/icons/Icon';
import { HeroStats } from '@/components/mypage/HeroStats';
import { ProfileCard } from '@/components/mypage/ProfileCard';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { ProjectCard } from '@/components/exhibition/ProjectCard';
import { BackLink } from '@/components/ui/BackLink';
import { Block, SideCard } from '@/components/ui/Block';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { fetchExhibitions, fetchUserProfile } from '@/lib/api';
import { MOCK_CURRENT_USER_ID } from '@/lib/mock';

export default async function PublicProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [profile, exhibitions] = await Promise.all([fetchUserProfile(id), fetchExhibitions()]);
  const myProjects = exhibitions.filter((project) => project.leader.id === profile.id);
  const isMe = profile.id === MOCK_CURRENT_USER_ID;
  const awards = profile.achievements.filter((item) => item.type === 'CONTEST');

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

        <HeroStats streakDays={profile.streakDays} badges={profile.badges} />

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
