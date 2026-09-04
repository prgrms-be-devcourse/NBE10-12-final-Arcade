package com.back.domain.member.profile.dtos;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;

import java.util.List;

public record MemberProfileDto(
    long id,
    String email,
    String name,
    String nickname,
    String webpage,
    /** 직접 올린 프로필 이미지. 올리지 않았으면 null 이다. 수정 요청에 되보내는 값도 이것이다 */
    String profileImageUrl,
    /**
     * GitHub 이 준 아바타. 서버가 둘을 합치지 않고 그대로 내려주니
     * 화면이 profileImageUrl ?? githubAvatarUrl 로 고르면 된다.
     */
    String githubAvatarUrl,
    String bio,
    /** 직접 적은 GitHub 사용자명. 커밋 작성자 매칭에 쓴다. OAuth 연동 여부는 githubLinked 로 따로 본다 */
    String githubUsername,
    /** GitHub 계정 연동 여부만 공개한다. provider 식별자·이메일은 응답에 포함하지 않는다. */
    boolean githubLinked,
    /** 대표 포지션 하나. 고르지 않았으면 null */
    PositionType position,
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
                profile.getProfileImageUrl(),
                profile.getMember().getProfileImgUrl(),
                profile.getBio(),
                profile.getGithubUsername(),
                profile.getMember().getGithubProviderUserId() != null
                        && !profile.getMember().getGithubProviderUserId().isBlank(),
                profile.getPosition(),
                profile.getTechStacks()
                        .stream()
                        .map(it -> it.getTechStack())
                        .toList(),
                profile.getCareers().stream().map(CareerDto::new).toList(),
                profile.getLinks().stream().map(ProfileLinkDto::new).toList()
        );
    }

}
