package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;

public record MemberLoginWithRefreshTokenDto(
    String accessToken,
    String refreshToken,
    String role,
    String grantType,
    int accessTokenExpiresIn
) {
    public MemberLoginWithRefreshTokenDto(Member member, String accessToken) {
        this(accessToken, member.getApiKey(), member.getRole().name(),
            "Bearer", 3600);
    }
}
