'use client';

import { useEffect, useState } from 'react';
import { fetchMyProfile, fetchHostCompany } from '@/lib/api';
import type { UserProfile } from '@/lib/types';

export interface CurrentUser {
  profile: UserProfile;
  /** 주최측 계정이면 소속 회사 id. 대회 수정·삭제 권한을 가르는 값이다 */
  hostId?: string;
}

/**
 * 로그인한 사용자.
 *
 * 아직 세션이 없어 목 프로필을 그대로 가져온다.
 * 화면은 이 값으로 '보여줄지'만 정하고, 실제 권한 판단은 서버가 한다.
 * 불러오는 동안에는 null 이라, 권한이 없는 사람에게 버튼이 잠깐 보였다 사라지는 일은 없다.
 */
export function useCurrentUser(): CurrentUser | null {
  const [user, setUser] = useState<CurrentUser | null>(null);

  useEffect(() => {
    let alive = true;

    fetchMyProfile()
      .then(async (profile) => {
        // 주최측 계정일 때만 소속을 추가로 확인한다
        const hostId =
          profile.memberRole === 'HOST'
            ? await fetchHostCompany()
                .then((company) => company.id)
                .catch(() => undefined)
            : undefined;
        if (alive) setUser({ profile, hostId });
      })
      .catch(() => {
        if (alive) setUser(null);
      });

    return () => {
      alive = false;
    };
  }, []);

  return user;
}
