package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private final PartyRepository partyRepository;

    public record PositionCreateSpec(
        PositionType type,
        int capacity
    ) { }

    @Transactional
    public PartyDto create(
        Member owner,
        String partyName,
        String title,
        String description,
        Long targetContestId,
        String contestName,
        String contestLinkUrl,
        TopicType topicType,
        PartyTag partyTag,
        String githubRepoUrl,
        int checklistRequiredApprovals,
        LocalDateTime deadline,
        List<PositionCreateSpec> positionSpecs
    ) {
        if (positionSpecs == null || positionSpecs.isEmpty()) {
            throw new ServiceException("400-4", "포지션 정원은 1명 이상이어야 합니다.");
        }

        positionSpecs.forEach(spec -> {
            if (spec.capacity() <= 0) {
                throw new ServiceException("400-4", "포지션 정원은 1명 이상이어야 합니다.");
            }
        });

        if (topicType == TopicType.CONTEST && targetContestId == null
            && (contestName == null || contestName.isBlank())) {
            throw new ServiceException("400-1", "등록된 대회가 없으면 대회명을 입력해야 합니다.");
        }

        Party party = new Party(
            owner,
            partyName,
            title,
            description,
            targetContestId,
            contestName,
            contestLinkUrl,
            topicType,
            partyTag,
            githubRepoUrl,
            checklistRequiredApprovals,
            deadline
        );

        positionSpecs.forEach(spec ->
            party.addPosition(new Position(spec.type(), spec.capacity()))
        );

        return new PartyDto(partyRepository.save(party));
    }
}
