package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;

public record MemberLoginDto(
    String accessToken,
    String role,
    String grantType,
    int accessTokenExpiresIn
) {
    public MemberLoginDto(String accessToken, String role) {
        this(accessToken, role, "Bearer", 3600);
    }

    public MemberLoginDto(Member member, String accessToken) {
        this(accessToken, member.getRole().name(),
            "Bearer", 3600);
    }
}
