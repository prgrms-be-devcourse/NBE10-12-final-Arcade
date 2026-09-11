package com.back.domain.member.member.dtos;

import com.back.domain.goal.goal.dtos.MemberAchievementItemDto;
import com.back.domain.party.party.dtos.MemberPartyHistoryDto;

import java.util.List;

public record MemberHistoryDto(
        MemberPartyHistoryDto partyHistory,
        List<MemberAchievementItemDto> achievements
) {
}
