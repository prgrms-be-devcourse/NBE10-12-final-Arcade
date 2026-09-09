package com.back.domain.member.profile.dtos;

import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 남의 공개 프로필. GET /members/{id} 가 돌려준다.
 *
 * 마이페이지(GET /me + /me/summary)와 거의 같지만 email·githubLinked 는 뺐다 - 본인 화면에서만 쓰는 값이다.
 * 반대로 집계는 /me 처럼 나누지 않고 한 번에 싣는다. 나눠 둔 이유가 '세션 확인용 GET /me 가 너무 자주 불린다'
 * 였는데, 이 경로는 남의 프로필을 열 때만 불려 그 사정이 없다.
 */
public record MemberPublicProfileDto(
        long id,
        String name,
        String nickname,
        String webpage,
        /** 직접 올린 프로필 이미지. 올리지 않았으면 null 이다 */
        String profileImageUrl,
        /** GitHub 아바타. 합치지 않고 그대로 내려주니 화면이 profileImageUrl ?? githubAvatarUrl 로 고른다 */
        String githubAvatarUrl,
        String bio,
        /** 대표 포지션 하나. 고르지 않았으면 null */
        PositionType position,
        List<String> techStacks,
        List<CareerDto> careers,
        List<ProfileLinkDto> links,
        long completedParties,
        long awards,
        long exhibitions,
        /** 연속 활동일. 프로필 카드와 '연속 활동' 히트맵이 함께 쓴다 */
        int streakDays,
        /** 최근 8주(56일) 활동 농도를 오래된 날부터 늘어놓은 0~3 값 */
        List<Integer> activityHeatmap,
        /**
         * 달성한 성취 전부. 타입을 가리지 않는다 - 기획서 3.7 이 성취에 공개 여부 필드를 두지 않고
         * 모든 Goal 을 전체 공개로 정했다. 화면이 CONTEST·PROJECT 를 나눠 그릴 뿐 서버는 골라내지 않는다.
         */
        List<GoalDto> achievements,
        /** 가입 시각. 프로필 카드의 '크루온 활동 N개월째' 가 여기서 나온다 */
        LocalDateTime joinedAt
) {
    public MemberPublicProfileDto(
            MemberProfile profile,
            MemberSummaryDto summary,
            List<GoalDto> achievements
    ) {
        this(
                profile.getMember().getId(),
                profile.getMember().getName(),
                profile.getNickname(),
                profile.getWebPage(),
                profile.getProfileImageUrl(),
                profile.getMember().getProfileImgUrl(),
                profile.getBio(),
                profile.getPosition(),
                profile.getTechStacks().stream().map(it -> it.getTechStack()).toList(),
                profile.getCareers().stream().map(CareerDto::new).toList(),
                profile.getLinks().stream().map(ProfileLinkDto::new).toList(),
                summary.completedParties(),
                summary.awards(),
                summary.exhibitions(),
                summary.streakDays(),
                summary.activityHeatmap(),
                achievements,
                summary.joinedAt()
        );
    }
}
