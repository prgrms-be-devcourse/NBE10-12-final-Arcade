package com.back.domain.party.recommendation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.recommendation.dtos.PartyRecommendationItemDto;
import com.back.domain.party.recommendation.dtos.PartyRecommendationResultDto;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyRecommendationService {

    private final PartyRecommendationRepository partyRecommendationRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final MemberProfileRepository memberProfileRepository;

    public PartyRecommendationResultDto getRecommendations(Member actor) {
        boolean hasProfile = memberProfileRepository.findByMember(actor).isPresent();
        if (!hasProfile) {
            return PartyRecommendationResultDto.needsProfile();
        }

        List<PartyRecommendation> recommendations = partyRecommendationRepository.findByMemberIdOrderByRank(actor.getId());
        if (recommendations.isEmpty()) {
            return PartyRecommendationResultDto.of(List.of());
        }

        List<Long> partyIds = recommendations.stream().map(PartyRecommendation::getPartyId).toList();

        // 배치 계산 이후 삭제된 파티가 있을 수 있어, 실제로 살아있는 파티만 골라낸다.
        Map<Long, Party> partyById = partyRepository.findAllByIdIn(partyIds).stream()
                .collect(Collectors.toMap(Party::getId, Function.identity()));
        Map<Long, Long> applicantCounts = partyMemberRepository.countApplicantsByPartyIds(partyIds);

        List<PartyRecommendationItemDto> items = recommendations.stream()
                .filter(rec -> partyById.containsKey(rec.getPartyId()))
                .map(rec -> {
                    Party party = partyById.get(rec.getPartyId());
                    return new PartyRecommendationItemDto(
                            new PartyListItemDto(party, applicantCounts.getOrDefault(party.getId(), 0L)),
                            rec.getReason()
                    );
                })
                .toList();

        return PartyRecommendationResultDto.of(items);
    }
}
