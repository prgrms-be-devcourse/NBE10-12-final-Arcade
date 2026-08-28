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
  return (
    <article className="project-card">
      <Link
        href={`/exhibition/${project.id}`}
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
          <Link href={`/exhibition/${project.id}`}>{project.title}</Link>
        </h5>
        <p className="sub">{project.summary}</p>
        <ChipRow>
          {project.skills.map((skill) => (
            <SkillChip key={skill}>{skill}</SkillChip>
          ))}
        </ChipRow>
        {showLeader ? (
          <LeaderRow user={project.leader} href={`/profile/${project.leader.id}`} />
        ) : null}
      </div>
    </article>
  );
}
