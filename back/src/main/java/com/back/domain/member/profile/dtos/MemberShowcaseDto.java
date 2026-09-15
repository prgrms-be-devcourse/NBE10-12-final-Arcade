package com.back.domain.member.profile.dtos;

import com.back.domain.party.showcase.entity.PartyShowcase;

import java.time.LocalDateTime;

/**
 * 공개 프로필 '참여한 프로젝트' 카드 한 장.
 *
 * 전시관 목록(GET /showcase/goals)을 쓰지 않는다. 그쪽은 응답에 소유자가 없어 화면에서 회원으로 거를 수 없고,
 * 같은 전시글이 팀원 수만큼 겹치는 걸 막으려고 파티당 대표 1건(goal.id 최소)만 남긴다 -
 * 회원으로 좁히면 대표가 아닌 참여자는 자기가 참여한 전시가 통째로 사라진다.
 *
 * 그래서 성취(Goal)가 아니라 전시글(PartyShowcase)에서 직접 고르고, 기준은 파티 확정 명단이다.
 * 요약의 '자동기록' 건수(countPublishedByAssembledMember)와 같은 조건이라 카드 수와 그 숫자가 어긋나지 않는다.
 */
public record MemberShowcaseDto(
        /** 전시 상세 경로가 쓰는 값. 전시는 파티에 종속이라 goal id 가 아니라 partyId 다 */
        long partyId,
        String partyName,
        /** 파티장이 붙인 전시 제목. 안 붙였으면 null 이라 화면이 파티명으로 대체한다 */
        String title,
        LocalDateTime publishedAt,
        int viewCount,
        int likeCount
) {
    public MemberShowcaseDto(PartyShowcase showcase) {
        this(
                showcase.getParty().getId(),
                showcase.getParty().getPartyName(),
                showcase.getTitle(),
                showcase.getPublishedAt(),
                showcase.getViewCount(),
                showcase.getLikeCount()
        );
    }
}
