package com.back.domain.party.application.dtos;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파티장 입장에서 본 받은 지원 한 건.
 * 파티장이 카드 하나에서 프로필과 성취를 함께 보고 승인/거절을 판단한다(기획서 2.1).
 */
public record ReceivedApplicationDto(
        long applicationId,
        long partyId,
        /** 화면이 파티+포지션 단위로 지원자를 묶으므로 이름이 함께 필요하다 */
        String partyName,
        Applicant applicant,
        ApplicationPositionDto position,
        PartyMemberStatus state,
        /** 지원 시 남긴 한 줄 메시지(50자). 안 남겼으면 null - 기획서 2.1 대로 카드 하단에 그대로 노출한다 */
        String message,
        List<Achievement> achievements,
        LocalDateTime createDate
) {
    public record Applicant(
            long id,
            String name,
            /** 프로필을 아직 만들지 않은 회원이면 null */
            String nickname,
            /**
             * 프로필의 대표 포지션 하나를 배열로 싣는다. 프로필에 position 은 단수 필드라
             * 원소가 둘 이상일 수 없고, 고르지 않았으면 빈 배열이다.
             */
            List<PositionType> positions,
            List<String> techStacks
    ) {
        public Applicant(PartyMember partyMember, MemberProfile profile) {
            this(
                    partyMember.getMember().getId(),
                    partyMember.getMember().getName(),
                    profile == null ? null : profile.getNickname(),
                    profile == null || profile.getPosition() == null
                            ? List.of()
                            : List.of(profile.getPosition()),
                    profile == null
                            ? List.of()
                            : profile.getTechStacks().stream().map(it -> it.getTechStack()).toList()
            );
        }
    }

    /**
     * 지원자 판단에 필요한 최소 정보만 싣는다.
     * 성취 전체 필드가 필요하면 성취 API 의 GoalDto 를 쓴다 - 그쪽 detail 은
     * 연결된 개인 TODO 를 지연 로딩해서, 목록에서 지원자 수만큼 추가 쿼리가 나간다.
     */
    public record Achievement(
            long goalId,
            GoalType type,
            GoalStatus status,
            GoalSource source,
            Detail detail
    ) {
        public record Detail(String title) { }

        public Achievement(Goal goal) {
            this(goal.getId(), goal.getType(), goal.getStatus(), goal.getSource(),
                    new Detail(goal.getTitle()));
        }
    }

    public ReceivedApplicationDto(PartyMember partyMember, MemberProfile profile, List<Goal> goals) {
        this(
                partyMember.getId(),
                partyMember.getParty().getId(),
                partyMember.getParty().getPartyName(),
                new Applicant(partyMember, profile),
                new ApplicationPositionDto(partyMember.getPosition()),
                partyMember.getStatus(),
                partyMember.getMessage(),
                goals.stream().map(Achievement::new).toList(),
                partyMember.getCreateDate()
        );
    }
}
