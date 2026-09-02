package com.back.domain.party.showcase.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.domain.party.showcase.dtos.PartyShowcaseDto;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.event.PartyShowcasePublishedEvent;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyShowcaseService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;
    private final PartyPrRepository partyPrRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PartyShowcaseDto getDraft(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElse(null);

        if (showcase == null || !showcase.isPublished()) {
            checkViewableAsDraft(party, actor);
        }

        List<String> memberNames = getApprovedMemberNames(party);
        List<PartyShowcaseDto.PrSummary> pullRequests = getPrSummaries(party.getId());

        return toDto(party, showcase, memberNames, pullRequests);
    }

    @Transactional
    public PartyShowcaseDto publish(long partyId, Member actor, String title, String description) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 게시할 수 있습니다.");
        }

        PartyShowcase showcase = partyShowcaseRepository.findByParty(party)
                .orElseGet(() -> new PartyShowcase(party));
        showcase.publish(title, description);
        partyShowcaseRepository.save(showcase);

        eventPublisher.publishEvent(new PartyShowcasePublishedEvent(party.getId(), title, description));

        List<String> memberNames = getApprovedMemberNames(party);
        List<PartyShowcaseDto.PrSummary> pullRequests = getPrSummaries(party.getId());

        return toDto(party, showcase, memberNames, pullRequests);
    }

    public List<PartyShowcaseDto> getTop3() {
        List<PartyShowcase> showcases = partyShowcaseRepository
                .findPublishedOrderByPartyLikeCountDesc(PageRequest.of(0, 3));

        if (showcases.isEmpty()) {
            return List.of();
        }

        List<Party> parties = showcases.stream().map(PartyShowcase::getParty).toList();
        List<Long> partyIds = parties.stream().map(Party::getId).toList();

        Map<Long, List<String>> memberNamesByPartyId = partyMemberRepository.findAllByPartyIn(parties).stream()
                .filter(pm -> pm.getStatus() == PartyMemberStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        pm -> pm.getParty().getId(),
                        Collectors.mapping(pm -> pm.getMember().getName(), Collectors.toList())
                ));

        Map<Long, List<PartyShowcaseDto.PrSummary>> pullRequestsByPartyId = partyPrRepository
                .findAllByPartyIdInOrderByGithubUpdatedAtDesc(partyIds).stream()
                .collect(Collectors.groupingBy(
                        pr -> pr.getParty().getId(),
                        Collectors.mapping(this::toPrSummary, Collectors.toList())
                ));

        return showcases.stream()
                .map(showcase -> toDto(
                        showcase.getParty(),
                        showcase,
                        memberNamesByPartyId.getOrDefault(showcase.getParty().getId(), List.of()),
                        pullRequestsByPartyId.getOrDefault(showcase.getParty().getId(), List.of())
                ))
                .toList();
    }

    private List<String> getApprovedMemberNames(Party party) {
        return partyMemberRepository.findAllByParty(party).stream()
                .filter(pm -> pm.getStatus() == PartyMemberStatus.APPROVED)
                .map(pm -> pm.getMember().getName())
                .toList();
    }

    private List<PartyShowcaseDto.PrSummary> getPrSummaries(long partyId) {
        return partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId).stream()
                .map(this::toPrSummary)
                .toList();
    }

    private PartyShowcaseDto.PrSummary toPrSummary(PartyPr pr) {
        return new PartyShowcaseDto.PrSummary(
                pr.getNumber(),
                pr.getTitle(),
                pr.getHtmlUrl(),
                pr.getState(),
                pr.getAuthorLogin(),
                pr.isMerged(),
                pr.getMergedAt()
        );
    }

    private PartyShowcaseDto toDto(
            Party party,
            PartyShowcase showcase,
            List<String> memberNames,
            List<PartyShowcaseDto.PrSummary> pullRequests
    ) {
        return new PartyShowcaseDto(
                party.getId(),
                party.getPartyName(),
                party.getOwner().getName(),
                memberNames,
                party.getGithubRepoUrl(),
                showcase != null ? showcase.getTitle() : null,
                showcase != null ? showcase.getDescription() : null,
                showcase != null && showcase.isPublished(),
                showcase != null ? showcase.getPublishedAt() : null,
                party.getViewCount(),
                party.getLikeCount(),
                pullRequests
        );
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    private void checkViewableAsDraft(Party party, Member actor) {
        if (party.isOwnedBy(actor)) {
            return;
        }
        boolean isApprovedMember = partyMemberRepository
                .existsByPartyAndMemberAndStatus(party, actor, PartyMemberStatus.APPROVED);
        if (!isApprovedMember) {
            throw new ServiceException("403-1", "게시되지 않은 전시는 파티원만 미리 볼 수 있습니다.");
        }
    }
}
