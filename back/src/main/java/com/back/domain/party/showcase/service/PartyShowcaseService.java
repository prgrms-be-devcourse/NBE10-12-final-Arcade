package com.back.domain.party.showcase.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.domain.party.showcase.dtos.PartyShowcaseDto;
import com.back.domain.party.showcase.dtos.PartyShowcaseListItemDto;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.event.PartyShowcasePublishedEvent;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public PartyShowcaseDto getDraft(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        // 한 번도 게시한 적 없으면 행 자체가 없을 수 있다 - 그래도 파티 정보만 채워서 초안으로 응답
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElse(null);

        // 이미 게시된 전시는 공개가 목적이라 누구나 볼 수 있다.
        // 아직 게시 전(초안)이면 파티장/파티원한테만 미리보기로 열어준다.
        if (showcase == null || !showcase.isPublished()) {
            checkViewableAsDraft(party, actor);
        } else {
            // 게시 전 미리보기(파티장/파티원)는 실제 방문이 아니라서 조회수에 안 잡히지만,
            // 게시된 뒤에는 누구든 볼 때마다 전시글 자체 조회수(PartyShowcase.viewCount)를 올린다.
            partyShowcaseRepository.increaseViewCount(showcase.getId());
            // increaseLikeCount/decreaseLikeCount와 같은 이유로 clearAutomatically=true가
            // 영속성 컨텍스트를 비워 위에서 들고 있던 showcase는 detached 상태로 남는다 -
            // 그대로 두면 toDto가 증가 전 조회수를 보여주므로, 다시 조회해서 최신값을 반영한다.
            showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        }

        List<String> memberNames = getApprovedMemberNames(party);
        List<PartyShowcaseDto.PrSummary> pullRequests = getPrSummaries(party.getId());

        return toDto(party, showcase, memberNames, pullRequests);
    }

    // 완료 시점에 바로 게시하든, 나중에 마이페이지에서 게시하든 파티 상태를 따로 검증하지 않는다
    // (기획서 34번 - "완료 시점 또는 마이페이지에서 선택적으로")
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

        // 이 파티의 Project(참여자 수만큼 존재) 전체에 title/description을 반영하는 건
        // 성취(Goal) 도메인이 이 이벤트를 구독해서 처리하면 되므로 여기서는 발행까지만
        eventPublisher.publishEvent(new PartyShowcasePublishedEvent(party.getId(), title, description));

        List<String> memberNames = getApprovedMemberNames(party);
        List<PartyShowcaseDto.PrSummary> pullRequests = getPrSummaries(party.getId());

        return toDto(party, showcase, memberNames, pullRequests);
    }

    // 홈 "인기 전시회 TOP3" - 파티마다 따로 쿼리 날리지 않고, 파티원/PR을 IN 절로 한 번에 모아온 뒤
    // 메모리에서 파티별로 묶어 조립한다 (party/owner는 리포지토리 쿼리에서 이미 fetch join됨)
    public List<PartyShowcaseDto> getTop3() {
        List<PartyShowcase> showcases = partyShowcaseRepository
                .findPublishedOrderByViewCountDesc(PageRequest.of(0, 3));

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

    // 전시관 목록 - 카드에 PR 목록/파티원 전체까지 안 실어도 되므로 무거운 조립 없이 바로 매핑
    public Page<PartyShowcaseListItemDto> getList(PartyTag partyTag, Pageable pageable) {
        return partyShowcaseRepository.findPublished(partyTag, pageable)
                .map(showcase -> new PartyShowcaseListItemDto(
                        showcase.getParty().getId(),
                        showcase.getParty().getPartyName(),
                        showcase.getTitle(),
                        showcase.getParty().getOwner().getName(),
                        showcase.getParty().getPartyTag(),
                        showcase.getParty().getViewCount(),
                        showcase.getParty().getLikeCount(),
                        showcase.getPublishedAt()
                ));
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
                // 전시글 좋아요/조회수는 모집글(Party)이 아니라 전시글(PartyShowcase) 자체 카운터를
                // 보여줘야 한다(기획서 3.2) - 게시 전(showcase==null)이면 아직 집계가 없으니 0
                showcase != null ? showcase.getViewCount() : 0,
                showcase != null ? showcase.getLikeCount() : 0,
                pullRequests
        );
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    private void checkViewableAsDraft(Party party, Member actor) {
        // 지금은 SecurityConfig가 이 엔드포인트를 인증 필수로 막아둬서 actor가 null일 수 없지만,
        // 그 설정에만 기대면 나중에 실수로 바뀌었을 때 403 대신 NPE(500)로 죽는다 - 방어적으로 처리
        if (actor == null) {
            throw new ServiceException("403-1", "게시되지 않은 전시는 파티원만 미리 볼 수 있습니다.");
        }
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
