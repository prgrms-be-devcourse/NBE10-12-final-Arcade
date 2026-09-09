package com.back.domain.party.party.dtos;

import com.back.domain.contest.contest.dtos.ContestSummaryDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.dtos.PositionDto;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record PartyDto(
    long id,
    long ownerId,
    String ownerName,
    String partyName,
    String title,
    String description,
    ContestSummaryDto targetContest,
    String contestTitle,
    /** 연결된 등록 대회의 형식(공모전/해커톤). 대회 파티가 아니거나 미등록 외부 대회면 null */
    ContestFormat contestFormat,
    String contestLinkUrl,
    TopicType topicType,
    PartyStatus status,
    PartyTag partyTag,
    String githubRepoUrl,
    LocalDateTime deadline,
    long dDay,
    int likeCount,
    int viewCount,
    /**
     * 지원한 사람 수 전체(거절 포함, 승인 인원 filledCount와는 다른 값).
     * 지원자 수를 세지 않는 응답(생성·수정·마감·완료)에서는 null - 0(지원자 없음)과 구분된다.
     */
    Long applicantCount,
    List<PositionDto> positions
) {
    // 지원자 수를 세지 않는 응답용. 세는 쪽(상세 조회)은 아래 생성자로 값을 넘긴다.
    public PartyDto(Party party) {
        this(party, null);
    }

    public PartyDto(Party party, Long applicantCount) {
        this(
            party.getId(),
            party.getOwner().getId(),
            party.getOwner().getName(),
            party.getPartyName(),
            party.getTitle(),
            party.getDescription(),
            party.getTargetContest() == null ? null : new ContestSummaryDto(party.getTargetContest()),
            party.getContestTitle(),
            party.getTargetContest() == null ? null : party.getTargetContest().getFormat(),
            party.getContestLinkUrl(),
            party.getTopicType(),
            party.getStatus(),
            party.getPartyTag(),
            party.getGithubRepoUrl(),
            party.getDeadline(),
            Duration.between(LocalDateTime.now(), party.getDeadline()).toDays(),
            party.getLikeCount(),
            party.getViewCount(),
            applicantCount,
            party.getPositions().stream().map(PositionDto::new).toList()
        );
    }
}
