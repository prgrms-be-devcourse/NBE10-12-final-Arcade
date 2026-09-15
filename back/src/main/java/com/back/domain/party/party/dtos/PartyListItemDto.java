package com.back.domain.party.party.dtos;

import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.dtos.PositionDto;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record PartyListItemDto(
        long id,
        String ownerName,
        String partyName,
        String title,
        TopicType topicType,
        /** 연결된 등록 대회의 형식(공모전/해커톤). 대회 파티가 아니거나 미등록 외부 대회면 null */
        ContestFormat contestFormat,
        PartyStatus status,
        PartyTag partyTag,
        LocalDateTime deadline,
        long dDay,
        int likeCount,
        int viewCount,
        /**
         * 지원한 사람 수 전체. 승인 인원(positions[].filledCount)과는 다른 값이다.
         * 이 값을 세지 않는 조회(홈 TOP3)에서는 null - 0(지원자 없음)과 구분된다.
         */
        Long applicantCount,
        List<PositionDto> positions,
        // 관리자 목록에서만 의미 있게 쓰인다. 일반 목록 조회는 애초에 hidden=true를 제외하고 가져오므로 항상 false.
        boolean hidden
) {
    // 지원자 수를 세지 않는 조회용. 세는 쪽은 아래 생성자로 값을 넘긴다.
    public PartyListItemDto(Party party) {
        this(party, null);
    }

    public PartyListItemDto(Party party, Long applicantCount) {
        this(
                party.getId(),
                party.getOwner().getName(),
                party.getPartyName(),
                party.getTitle(),
                party.getTopicType(),
                party.getTargetContest() == null ? null : party.getTargetContest().getFormat(),
                party.getStatus(),
                party.getPartyTag(),
                party.getDeadline(),
                Duration.between(LocalDateTime.now(), party.getDeadline()).toDays(),
                party.getLikeCount(),
                party.getViewCount(),
                applicantCount,
                party.getPositions().stream().map(PositionDto::new).toList(),
                party.isHidden()
        );
    }
}
