package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.profile.entity.MemberProfile;

import java.time.LocalDateTime;

public record MemberListItemDto(
        long id,
        String email,
        String name,
        String nickname,
        Role role,
        boolean active,
        LocalDateTime createDate
) {
    public MemberListItemDto(Member member, MemberProfile profile) {
        this(
                member.getId(),
                member.getEmail(),
                member.getName(),
                profile == null ? null : profile.getNickname(),
                member.getRole(),
                member.isActive(),
                member.getCreateDate()
        );
    }
}
