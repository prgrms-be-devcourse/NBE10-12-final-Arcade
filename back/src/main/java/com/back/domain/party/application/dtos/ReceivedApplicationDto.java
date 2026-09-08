package com.back.domain.party.application.dtos;

import com.back.domain.goal.goal.dtos.OwnerAchievementCount;
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
        PositionType position,
        PartyMemberStatus state,
        /** 지원 시 남긴 한 줄 메시지(50자). 안 남겼으면 null - 기획서 2.1 대로 카드 하단에 그대로 노출한다 */
        String message,
        AchievementSummary achievements,
        LocalDateTime createDate
) {
    public record Applicant(
            long id,
            String name,
            /** 프로필을 아직 만들지 않은 회원이면 null */
            String nickname,
            /**
             * 프로필에 적어둔 희망 포지션. 고르지 않았거나 프로필이 없으면 null 이다.
             * 바깥의 position(이번에 지원한 포지션)과는 다른 값이라 이름을 나눴다.
             */
            PositionType preferredPosition,
            List<String> techStacks
    ) {
        public Applicant(PartyMember partyMember, MemberProfile profile) {
            this(
                    partyMember.getMember().getId(),
                    partyMember.getMember().getName(),
                    profile == null ? null : profile.getNickname(),
                    profile == null ? null : profile.getPosition(),
                    profile == null
                            ? List.of()
                            : profile.getTechStacks().stream().map(it -> it.getTechStack()).toList()
            );
        }
    }

    /**
     * 성취는 건수만 싣는다 - 카드에서 판단에 쓰는 건 "얼마나 쌓았는지"와 그 출처지, 개별 제목이 아니다.
     * 목록이 필요하면 지원자 프로필로 들어가 성취 API 를 쓴다.
     */
    public record AchievementSummary(
            /** 크루온 활동으로 자동기록된 건 - 지금은 파티 확정으로 생기는 PROJECT 뿐이다 */
            long platformVerified,
            /** 사용자가 직접 등록한 건 - 수상·체크리스트 */
            long selfReported
    ) {
        private static final AchievementSummary EMPTY = new AchievementSummary(0, 0);

        public static AchievementSummary from(OwnerAchievementCount count) {
            return count == null
                    ? EMPTY
                    : new AchievementSummary(count.platformVerified(), count.selfReported());
        }
    }

    public ReceivedApplicationDto(PartyMember partyMember, MemberProfile profile, OwnerAchievementCount achievements) {
        this(
                partyMember.getId(),
                partyMember.getParty().getId(),
                partyMember.getParty().getPartyName(),
                new Applicant(partyMember, profile),
                partyMember.getPosition().getType(),
                partyMember.getStatus(),
                partyMember.getMessage(),
                AchievementSummary.from(achievements),
                partyMember.getCreateDate()
        );
    }
}
