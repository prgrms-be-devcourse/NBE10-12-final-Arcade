package com.back.domain.member.member.dtos;

import com.back.domain.member.member.entity.Member;

public record MemberDto(
    long id,
    String email
) {
    public MemberDto(Member member) {
        this(member.getId(), member.getEmail());
    }
}
