package com.back.domain.party.showcase.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyShowcaseService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;
    private final PartyPrRepository partyPrRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PartyShowcaseDto getDraft(long partyId) {
        Party party = findPartyOrThrow(partyId);
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElse(null);
        return toDto(party, showcase);
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

        return toDto(party, showcase);
    }

    public List<PartyShowcaseDto> getTop3() {
        return partyShowcaseRepository.findPublishedOrderByPartyLikeCountDesc(PageRequest.of(0, 3)).stream()
                .map(showcase -> toDto(showcase.getParty(), showcase))
                .toList();
    }

    private PartyShowcaseDto toDto(Party party, PartyShowcase showcase) {
        List<String> memberNames = partyMemberRepository.findAllByParty(party).stream()
                .filter(pm -> pm.getStatus() == PartyMemberStatus.APPROVED)
                .map(pm -> pm.getMember().getName())
                .toList();

        List<PartyShowcaseDto.PrSummary> pullRequests = partyPrRepository
                .findAllByPartyIdOrderByGithubUpdatedAtDesc(party.getId()).stream()
                .map(pr -> new PartyShowcaseDto.PrSummary(
                        pr.getNumber(),
                        pr.getTitle(),
                        pr.getHtmlUrl(),
                        pr.getState(),
                        pr.getAuthorLogin(),
                        pr.isMerged(),
                        pr.getMergedAt()
                ))
                .toList();

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
}
