package com.back.domain.interaction.bookmark.dtos;

import com.back.domain.interaction.like.entity.TargetType;

import java.time.LocalDateTime;

/**
 * 북마크함 카드 한 장. 파티·대회·전시를 한 목록에 섞어 내려준다(기획서 2.11).
 *
 * target 은 targetType 에 따라 모양이 다르다 - 각 목록 API 가 이미 쓰는 카드 DTO 를 그대로 싣는다.
 * PARTY 는 PartyListItemDto, CONTEST 는 ContestResponseDto, PARTY_SHOWCASE 는 ShowcaseGoalDto 라서
 * 화면이 파티 목록·대회 허브·전시관에서 쓰던 카드 컴포넌트를 그대로 재사용할 수 있다.
 * 셋이 공통 상위 타입을 갖지 않아(각기 다른 도메인) Object 로 둔다 - 어느 모양인지는 targetType 이 말해준다.
 */
public record MyBookmarkDto(
        long id,
        TargetType targetType,
        Object target,
        LocalDateTime bookmarkedAt
) {
}
