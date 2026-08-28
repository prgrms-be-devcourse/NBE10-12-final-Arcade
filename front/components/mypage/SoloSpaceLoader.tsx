'use client';

import { useEffect, useState } from 'react';
import { SoloSpace } from './SoloSpace';
import { fetchSoloSpace } from '@/lib/api';
import type { ChecklistItem } from '@/lib/types';

interface SoloSpaceData {
  id: string;
  title: string;
  type: string;
  createdAt: string;
  memo: string;
  checklist: ChecklistItem[];
}

/**
 * 개인 TODO 상세는 클라이언트에서 조회한다.
 * 목 모드에서는 방금 만든 항목이 브라우저 쪽 저장소에만 있어, 서버에서 찾으면 비어 보이기 때문이다.
 * 실제 API 로 바뀌어도 호출부는 그대로다.
 */
export function SoloSpaceLoader({ id, ownerName }: { id: string; ownerName: string }) {
  const [space, setSpace] = useState<SoloSpaceData | null>(null);

  useEffect(() => {
    let alive = true;
    fetchSoloSpace(id).then((data) => {
      if (alive) setSpace(data as SoloSpaceData);
    });
    return () => {
      alive = false;
    };
  }, [id]);

  if (!space) {
    return <p className="notif-empty">목록을 불러오는 중이에요.</p>;
  }

  return <SoloSpace space={space} ownerName={ownerName} />;
}
