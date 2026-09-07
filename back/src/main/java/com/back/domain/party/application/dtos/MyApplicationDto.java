package com.back.domain.party.application.dtos;

import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.LocalDateTime;

/** 지원자 입장에서 본 내 지원 한 건. 파티로 이동해야 해서 partyId 를 party 객체째로 싣는다. */
public record MyApplicationDto(
        long applicationId,
        PartySummary party,
        ApplicationPositionDto position,
        PartyMemberStatus state,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public record PartySummary(
            long id,
            /** 카드 제목이자 목록에서 파티를 구분하는 이름 */
            String name,
            long ownerId,
            TopicType topicType,
            /** 연결된 등록 대회의 분야. 대회 파티가 아니거나 미등록 외부 대회면 null 이다. */
            ContestTag contestTag,
            PartyStatus status
    ) {
        public PartySummary(Party party) {
            this(
                    party.getId(),
                    party.getPartyName(),
                    party.getOwner().getId(),
                    party.getTopicType(),
                    party.getTargetContest() == null ? null : party.getTargetContest().getContestTag(),
                    party.getStatus()
            );
        }
    }

    public MyApplicationDto(PartyMember partyMember) {
        this(
                partyMember.getId(),
                new PartySummary(partyMember.getParty()),
                new ApplicationPositionDto(partyMember.getPosition()),
                partyMember.getStatus(),
                partyMember.getCreateDate(),
                partyMember.getModifyDate()
        );
    }
}
