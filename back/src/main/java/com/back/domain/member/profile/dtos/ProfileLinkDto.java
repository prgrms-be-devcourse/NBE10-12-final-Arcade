package com.back.domain.member.profile.dtos;

import com.back.domain.member.profile.entity.MemberProfileLink;

public record ProfileLinkDto(long id, String label, String url) {

    public ProfileLinkDto(MemberProfileLink link) {
        this(link.getId(), link.getLabel(), link.getUrl());
    }
}
