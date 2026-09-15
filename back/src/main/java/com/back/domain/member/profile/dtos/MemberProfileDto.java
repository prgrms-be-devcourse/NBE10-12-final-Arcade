package com.back.domain.member.profile.dtos;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.profile.entity.MemberProfile;

import java.time.LocalDateTime;
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
    /** GitHub 계정 연동 여부만 공개한다. provider 식별자·이메일은 응답에 포함하지 않는다. */
    boolean githubLinked,
    /**
     * 계정 권한. 화면의 대표 포지션(position)과 다른 개념이다.
     *
     * 예전에는 응답에 없어 화면이 로그인 응답에서 받아 덮어썼는데,
     * 그러면 새로고침 뒤에는 알 수 없어 관리자에게도 회원용 온보딩이 뜬다.
     */
    Role role,
    /** 대표 포지션 하나. 고르지 않았으면 null */
    PositionType position,
    List<String> techStacks,
    List<CareerDto> careers,
    List<ProfileLinkDto> links,
    /**
     * 이용약관 동의 일시. null 이면 아직 동의하지 않은 계정이다.
     * 화면은 이 값으로 온보딩(약관 동의) 화면을 띄울지 판단한다.
     */
    LocalDateTime termsAgreedAt,
    /** 개인정보 수집·이용 동의 일시. null 이면 온보딩이 필요하다 */
    LocalDateTime privacyAgreedAt
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
                profile.getMember().getGithubProviderUserId() != null
                        && !profile.getMember().getGithubProviderUserId().isBlank(),
                profile.getMember().getRole(),
                profile.getPosition(),
                profile.getTechStacks()
                        .stream()
                        .map(it -> it.getTechStack())
                        .toList(),
                profile.getCareers().stream().map(CareerDto::new).toList(),
                profile.getLinks().stream().map(ProfileLinkDto::new).toList(),
                profile.getMember().getTermsAgreedAt(),
                profile.getMember().getPrivacyAgreedAt()
        );
    }

}
