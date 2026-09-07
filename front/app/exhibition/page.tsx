import { ExhibitionBoard } from '@/components/exhibition/ExhibitionBoard';
import { LinkButton } from '@/components/ui/Button';
import { SectionHead } from '@/components/ui/SectionHead';
import { fetchExhibitions } from '@/lib/api';
import { GOAL_TYPE_LABELS } from '@/lib/constants';

// 서버 전시 목록에는 분야 태그가 없고 성취 타입만 있다(GET /showcase/goals).
const EXHIBITION_CATEGORIES = ['전체', ...Object.values(GOAL_TYPE_LABELS)];

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
        <ExhibitionBoard projects={projects} categories={EXHIBITION_CATEGORIES} />
      </div>
    </main>
  );
}
