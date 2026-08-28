import { ContestDetailView } from '@/components/contest/ContestDetailView';

export default async function ContestDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ContestDetailView id={id} />;
}
