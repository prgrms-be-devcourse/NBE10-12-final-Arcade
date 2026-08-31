package com.back.domain.party.application.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.dtos.PartyApplicationDto;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyApplicationService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;

    @Transactional
    public PartyApplicationDto apply(long partyId, long positionId, Member applicant, String message) {
        Party party = findPartyOrThrow(partyId);

        if (party.isOwnedBy(applicant)) {
            throw new ServiceException("409-2", "본인이 만든 파티에는 지원할 수 없습니다.");
        }

        if (party.getStatus() != PartyStatus.RECRUITING) {
            throw new ServiceException("409-3", "모집이 종료된 파티입니다.");
        }

        if (partyMemberRepository.existsByPartyAndMember(party, applicant)) {
            throw new ServiceException("409-1", "이미 지원했거나 참여 중인 파티입니다.");
        }

        Position position = party.findPosition(positionId);

        PartyMember partyMember = new PartyMember(party, applicant, position, message);
        return new PartyApplicationDto(partyMemberRepository.save(partyMember));
    }

    public List<PartyApplicationDto> getApplications(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 조회할 수 있습니다.");
        }

        return partyMemberRepository.findAllByParty(party).stream()
                .map(PartyApplicationDto::new)
                .toList();
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }
}
