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
    /** GitHub 계정 연동 여부만 공개한다. provider 식별자·이메일은 응답에 포함하지 않는다. */
    boolean githubLinked,
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
                profile.getMember().getGithubProviderUserId() != null
                        && !profile.getMember().getGithubProviderUserId().isBlank(),
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
