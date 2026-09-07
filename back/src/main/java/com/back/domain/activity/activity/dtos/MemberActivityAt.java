package com.back.domain.activity.activity.dtos;

import java.time.LocalDateTime;

/**
 * 백필이 과거 활동을 훑을 때 쓰는 최소 투영 - 누가, 언제.
 *
 * 엔티티로 읽으면 파티·지원·좋아요·북마크를 통째로 메모리에 올리게 되는데,
 * 백필에 필요한 건 이 두 값뿐이다.
 */
public record MemberActivityAt(Long memberId, LocalDateTime at) {
}
