package com.back.domain.member.profile.dtos;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;

import java.util.List;

public record MemberProfileDto(
    long id,
    String email,
    String name,
    String nickname,
    String webpage,
    String profileImageUrl,
    List<String> positions,
    List<String> techStacks
) {
    public MemberProfileDto(MemberProfile profile) {
        this(
                profile.getMember().getId(),
                profile.getMember().getEmail(),
                profile.getMember().getName(),
                profile.getNickname(),
                profile.getWebPage(),
                profile.getMember().getProfileImgUrl(),
                profile.getPositions()
                        .stream()
                        .map(it -> it.getPositionType().name())
                        .toList(),
                profile.getTechStacks()
                        .stream()
                        .map(it -> it.getTechStack())
                        .toList()
        );
    }

}
