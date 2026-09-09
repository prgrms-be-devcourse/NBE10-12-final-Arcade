package com.back.domain.party.partyPr.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** PR read model and access checks. Webhook/sync writes remain in PartyPrService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyPrQueryService {
    private final PartyPrRepository partyPrRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;

    public List<PartyPrDto> getByPartyId(long partyId, Member actor) {
        readableParty(partyId, actor);
        return partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId).stream().map(PartyPrDto::new).toList();
    }

    public List<PartyPrByMemberDto> getByPartyIdGroupedByMember(long partyId, Member actor) {
        Party party = readableParty(partyId, actor);
        Map<Long, Member> members = new LinkedHashMap<>();
        members.put(party.getOwner().getId(), party.getOwner());
        partyMemberRepository.findAllByParty(party).stream().filter(pm -> pm.getStatus() == PartyMemberStatus.APPROVED)
                .forEach(pm -> members.put(pm.getMember().getId(), pm.getMember()));
        Map<Long, List<PartyPrDto>> grouped = new LinkedHashMap<>();
        Map<Long, String> logins = new LinkedHashMap<>();
        List<PartyPrDto> unknown = new ArrayList<>();
        for (PartyPr pr : partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId)) {
            PartyPrDto dto = new PartyPrDto(pr);
            if (pr.getAuthorGithubUserId() == null) { unknown.add(dto); continue; }
            grouped.computeIfAbsent(pr.getAuthorGithubUserId(), ignored -> new ArrayList<>()).add(dto);
            logins.putIfAbsent(pr.getAuthorGithubUserId(), pr.getAuthorLogin());
        }
        List<PartyPrByMemberDto> result = new ArrayList<>();
        Set<Long> matched = new HashSet<>();
        for (Member member : members.values()) {
            Long githubUserId = member.getGithubUserId();
            if (githubUserId != null) matched.add(githubUserId);
            result.add(PartyPrByMemberDto.member(member, party.isOwnedBy(member),
                    githubUserId == null ? null : logins.get(githubUserId),
                    githubUserId == null ? List.of() : grouped.getOrDefault(githubUserId, List.of())));
        }
        grouped.forEach((id, prs) -> { if (!matched.contains(id)) result.add(PartyPrByMemberDto.external(id, logins.get(id), prs)); });
        if (!unknown.isEmpty()) result.add(PartyPrByMemberDto.external(null, null, unknown));
        return result;
    }

    public PartyPrByMemberDto getByPartyIdAndMemberId(long partyId, long memberId, Member actor) {
        Party party = readableParty(partyId, actor);
        Member member = party.getOwner().getId() == memberId ? party.getOwner() : partyMemberRepository
                .findByParty_IdAndMember_IdAndStatus(partyId, memberId, PartyMemberStatus.APPROVED)
                .map(PartyMember::getMember).orElseThrow(() -> new ServiceException("404-2", "파티장 또는 승인된 파티원을 찾을 수 없습니다."));
        List<PartyPrDto> prs = member.getGithubUserId() == null ? List.of() : partyPrRepository
                .findAllByPartyIdAndAuthorGithubUserIdOrderByGithubUpdatedAtDesc(partyId, member.getGithubUserId()).stream().map(PartyPrDto::new).toList();
        return PartyPrByMemberDto.member(member, party.isOwnedBy(member), prs.isEmpty() ? null : prs.getFirst().authorLogin(), prs);
    }

    public List<PartyPrDto> getMyPullRequests(Member actor) {
        if (actor == null || actor.getGithubUserId() == null) throw new ServiceException("400-20", "GITHUB_ACCOUNT_LINK_REQUIRED");
        return partyPrRepository.findAllByAuthorGithubUserIdOrderByGithubUpdatedAtDesc(actor.getGithubUserId()).stream()
                .filter(pr -> canRead(pr.getParty(), actor)).map(PartyPrDto::new).toList();
    }

    private Party readableParty(long partyId, Member actor) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
        if (!canRead(party, actor)) throw new ServiceException("403-1", "파티장 또는 확정 파티원만 PR을 조회할 수 있습니다.");
        return party;
    }

    private boolean canRead(Party party, Member actor) {
        return actor != null && (party.isOwnedBy(actor) || partyMemberRepository.existsByPartyAndMemberAndStatus(party, actor, PartyMemberStatus.APPROVED));
    }
}
