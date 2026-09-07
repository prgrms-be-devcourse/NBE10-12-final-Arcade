import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { LeaderRow } from '@/components/ui/Avatar';
import { ChipRow, SkillChip, SourceBadge } from '@/components/ui/Tag';
import type { ExhibitionProject } from '@/lib/types';

interface ProjectCardProps {
  project: ExhibitionProject;
  /** 홈 인기 전시회 섹션에서 순위 배지를 표시 */
  rank?: number;
  showLeader?: boolean;
}

/** 전시관 프로젝트 카드 */
export function ProjectCard({ project, rank, showLeader = true }: ProjectCardProps) {
  /*
   * 전시 상세는 두 갈래다.
   * - PROJECT : 파티에 종속이라 파티 전시 상세로 (GET /parties/{partyId}/showcase)
   * - 그 외   : 개인 성취라 파티가 없다. 성취 상세로 (GET /goals/{goalId})
   */
  const href = project.sourcePartyId
    ? `/exhibition/${project.sourcePartyId}`
    : `/goals/${project.id}`;

  return (
    <article className="project-card">
      <Link
        href={href}
        className={project.coverImageUrl ? 'project-thumb has-cover' : 'project-thumb'}
        style={
          project.coverImageUrl ? { backgroundImage: `url(${project.coverImageUrl})` } : undefined
        }
      >
        {rank ? <span className="exh-rank">{rank}</span> : null}
        <span className="exh-metrics">
          <span className="exh-views">
            <Icon name="i-eye" />
            {project.viewCount.toLocaleString()}
          </span>
          <span className="exh-like">
            <Icon name="i-heart" />
            {project.likeCount}
          </span>
        </span>
      </Link>
      <div className="project-body">
        <SourceBadge source={project.source} />
        <h5>
          <Link href={href}>{project.title}</Link>
        </h5>
        <p className="sub">{project.summary}</p>
        <ChipRow>
          {project.skills.map((skill) => (
            <SkillChip key={skill}>{skill}</SkillChip>
          ))}
        </ChipRow>
        {showLeader && project.leader ? (
          <LeaderRow user={project.leader} href={`/profile/${project.leader.id}`} />
        ) : null}
      </div>
    </article>
  );
}
