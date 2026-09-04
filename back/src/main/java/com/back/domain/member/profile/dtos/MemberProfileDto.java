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
    String bio,
    /** GitHub 계정 연동 여부만 공개한다. provider 식별자·이메일은 응답에 포함하지 않는다. */
    boolean githubLinked,
    List<String> positions,
    List<String> techStacks,
    List<CareerDto> careers,
    List<ProfileLinkDto> links
) {
    public MemberProfileDto(MemberProfile profile) {
        this(
                profile.getMember().getId(),
                profile.getMember().getEmail(),
                profile.getMember().getName(),
                profile.getNickname(),
                profile.getWebPage(),
                // 직접 올린 이미지가 있으면 그것, 없으면 OAuth 가 준 아바타
                profile.getProfileImageUrl() != null
                        ? profile.getProfileImageUrl()
                        : profile.getMember().getProfileImgUrl(),
                profile.getBio(),
                profile.getMember().getGithubProviderUserId() != null
                        && !profile.getMember().getGithubProviderUserId().isBlank(),
                profile.getPositions()
                        .stream()
                        .map(it -> it.getPositionType().name())
                        .toList(),
                profile.getTechStacks()
                        .stream()
                        .map(it -> it.getTechStack())
                        .toList(),
                profile.getCareers().stream().map(CareerDto::new).toList(),
                profile.getLinks().stream().map(ProfileLinkDto::new).toList()
        );
    }

}
