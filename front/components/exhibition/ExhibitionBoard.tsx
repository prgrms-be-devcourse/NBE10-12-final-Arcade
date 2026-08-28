'use client';

import { useState } from 'react';
import { ProjectCard } from './ProjectCard';
import { FilterBlock, FilterChips } from '@/components/ui/FilterChips';
import type { ExhibitionProject } from '@/lib/types';

export function ExhibitionBoard({
  projects,
  categories,
}: {
  projects: ExhibitionProject[];
  categories: readonly string[];
}) {
  const [category, setCategory] = useState('전체');

  const visible =
    category === '전체' ? projects : projects.filter((project) => project.category === category);

  return (
    <>
      <FilterBlock>
        <FilterChips label="분야" options={categories} value={category} onChange={setCategory} />
      </FilterBlock>

      <div className="board-grid">
        {visible.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
      </div>
      {visible.length === 0 ? <p className="notif-empty">조건에 맞는 전시가 없어요.</p> : null}
    </>
  );
}
