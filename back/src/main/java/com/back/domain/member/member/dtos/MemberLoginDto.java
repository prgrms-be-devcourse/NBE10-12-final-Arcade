package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;

public record MemberLoginDto(
    String accessToken,
    String refreshToken,
    String role,
    String grantType,
    int accessTokenExpiresIn
) {
    public MemberLoginDto(
        Member member,
        String accessToken,
        String refreshToken,
        int accessTokenExpiresIn
    ) {
        this(accessToken, refreshToken, member.getRole().name(),
            "Bearer", accessTokenExpiresIn);
    }
}
