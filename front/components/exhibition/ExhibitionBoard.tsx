'use client';

import { useState } from 'react';
import { ProjectCard } from './ProjectCard';
import { SelectField } from '@/components/ui/Field';
import { fetchExhibitions } from '@/lib/api';
import type { ExhibitionProject } from '@/lib/types';

type ExhibitionSort = 'like' | 'recent';

const SORT_LABELS: Record<ExhibitionSort, string> = {
  like: '인기순',
  recent: '최신순',
};

export function ExhibitionBoard({ projects: initial }: { projects: ExhibitionProject[] }) {
  const [projects, setProjects] = useState(initial);
  const [sort, setSort] = useState<ExhibitionSort>('like');
  const [loading, setLoading] = useState(false);

  const changeSort = async (next: ExhibitionSort) => {
    setSort(next);
    setLoading(true);
    try {
      setProjects(await fetchExhibitions(next));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="sort-row">
        <SelectField
          className="select-field"
          value={sort}
          onChange={(event) => changeSort(event.target.value as ExhibitionSort)}
          aria-label="정렬"
        >
          {(Object.entries(SORT_LABELS) as [ExhibitionSort, string][]).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
      </div>
      <div className="board-grid">
        {projects.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
      </div>
      {loading ? null : projects.length === 0 ? (
        <p className="notif-empty">아직 게시된 전시가 없어요.</p>
      ) : null}
    </>
  );
}
