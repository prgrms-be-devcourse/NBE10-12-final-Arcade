import { ProjectCard } from './ProjectCard';
import type { ExhibitionProject } from '@/lib/types';

export function ExhibitionBoard({ projects }: { projects: ExhibitionProject[] }) {
  return (
    <>
      <div className="board-grid">
        {projects.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
      </div>
      {projects.length === 0 ? <p className="notif-empty">아직 게시된 전시가 없어요.</p> : null}
    </>
  );
}
