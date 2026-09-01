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
 * 화면은 이 값으로 '보여줄지'만 정하고, 실제 권한 판단은 서버가 한다.
 *
 * 세 가지 상태를 구분한다.
 * - undefined : 확인 중. 아직 아무것도 단정하지 않는다
 * - null      : 비로그인. GET /members/me 가 401 을 돌려준 경우다
 *               (지금은 서버 토큰 버그로 500 이 나는 경우도 여기로 들어온다)
 * - CurrentUser : 로그인됨
 *
 * '확인 중'을 따로 두는 이유는, 로그인한 사람에게 로그인 버튼이 잠깐 스쳤다 사라지는 걸 막기 위해서다.
 */
export function useCurrentUser(): CurrentUser | null | undefined {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);

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
