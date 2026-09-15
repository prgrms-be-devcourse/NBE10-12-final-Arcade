package com.back.domain.party.recommendation.curation;

import com.back.domain.member.member.entity.PositionType;

import java.util.List;

/** 큐레이션 프롬프트에 넣을 회원 요약 - 프로필 포지션/기술스택 + 최근 검색어. */
public record MemberCurationContext(
        PositionType position,
        List<String> techStacks,
        List<String> recentSearchKeywords
) {
}
