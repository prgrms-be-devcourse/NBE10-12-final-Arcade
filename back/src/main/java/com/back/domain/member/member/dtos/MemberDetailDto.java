package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.entity.MemberProfileTechStack;

import java.time.LocalDateTime;
import java.util.List;

public record MemberDetailDto(
        long id,
        String email,
        String name,
        String nickname,
        Role role,
        boolean active,
        String suspendReason,
        PositionType position,
        List<String> techStacks,
        LocalDateTime createDate
) {
    public MemberDetailDto(Member member, MemberProfile profile) {
        this(
                member.getId(),
                member.getEmail(),
                member.getName(),
                profile == null ? null : profile.getNickname(),
                member.getRole(),
                member.isActive(),
                member.getSuspendReason(),
                profile == null ? null : profile.getPosition(),
                profile == null
                        ? List.of()
                        : profile.getTechStacks().stream().map(MemberProfileTechStack::getTechStack).toList(),
                member.getCreateDate()
        );
    }
}
