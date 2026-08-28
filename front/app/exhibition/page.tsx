import { ExhibitionBoard } from '@/components/exhibition/ExhibitionBoard';
import { LinkButton } from '@/components/ui/Button';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchExhibitions } from '@/lib/api';
import { MOCK_EXHIBITION_CATEGORIES } from '@/lib/mock';

export default async function ExhibitionPage() {
  const projects = await fetchExhibitions();

  return (
    <main>
      <div className="board-wrap container">
        <SectionHead
          title="전시관"
          description="크루온에서 완료된 프로젝트를 만나보세요. 카드를 클릭하면 상세 페이지로, 만든 사람을 클릭하면 프로필로 이동합니다."
          action={<LinkButton href="/exhibition/create">전시 등록</LinkButton>}
        />
        <ExhibitionBoard projects={projects} categories={MOCK_EXHIBITION_CATEGORIES} />
      </div>
    </main>
  );
}
