import { notFound } from 'next/navigation';
import { BackLink } from '@/components/ui/BackLink';
import { LEGAL_DOCUMENTS, findLegalDocument } from '@/lib/legal';

/** /legal/terms · /legal/privacy 만 열린다 */
export function generateStaticParams() {
  return LEGAL_DOCUMENTS.map((document) => ({ key: document.key }));
}

export default async function LegalPage({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  const document = findLegalDocument(key);

  if (!document) notFound();

  return (
    <main>
      <div className="legal-wrap">
        <BackLink />
        <h2>{document.title}</h2>
        {/* 문안이 평문이라 줄바꿈을 그대로 살린다 */}
        <div className="legal-body">{document.body}</div>
      </div>
    </main>
  );
}
