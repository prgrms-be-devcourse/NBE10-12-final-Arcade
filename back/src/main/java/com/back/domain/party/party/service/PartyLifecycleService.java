package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.assemble.entity.PartyAssemble;
import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.event.PartyAssembledEvent;
import com.back.domain.party.party.event.PartyCompletedEvent;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyLifecycleService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyAssembleRepository partyAssembleRepository;
    private final PartyAssembleToMemberRepository partyAssembleToMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PartyDto closeRecruiting(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 처리할 수 있습니다.");
        }

        party.closeRecruiting(); // RECRUITING 아니면 409-1

        List<PartyMember> members = partyMemberRepository.findAllByParty(party);

        // 그 시점까지 판정 안 된 지원 건은 일괄 거절
        members.stream()
                .filter(m -> m.getStatus() == PartyMemberStatus.PENDING)
                .forEach(PartyMember::reject);

        // Member만 뽑지 않고 PartyMember를 그대로 들고 있어야 positionType을 이벤트에 실을 수 있다
        List<PartyMember> approvedPartyMembers = members.stream()
                .filter(m -> m.getStatus() == PartyMemberStatus.APPROVED)
                .toList();

        // 파티 확정 원본 사건과 그 시점 승인된 참여자별 파생 레코드를 분리 기록
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(party));
        List<PartyAssembleToMember> assembleToMembers = approvedPartyMembers.stream()
                .map(pm -> new PartyAssembleToMember(partyAssemble, pm.getMember()))
                .toList();
        partyAssembleToMemberRepository.saveAll(assembleToMembers);

        LocalDate assembledAt = LocalDate.now();

        // approvedPartyMembers와 assembleToMembers는 같은 순서로 만들어졌으니 인덱스로 짝지어 memberId + 그 사람의 PartyAssembleToMember id + positionType을 한 번에 이벤트에 실어 보낸다
        List<PartyAssembledEvent.ApprovedMember> approvedMembersPayload = IntStream.range(0, approvedPartyMembers.size())
                .mapToObj(i -> new PartyAssembledEvent.ApprovedMember(
                        approvedPartyMembers.get(i).getMember().getId(),
                        assembleToMembers.get(i).getId(),
                        approvedPartyMembers.get(i).getPosition().getType()
                ))
                .toList();

        // 성취 자동생성, 체크리스트 오픈은 해당 도메인이 리스너를 붙이면 되므로 여기서는 이벤트 발행까지만
        eventPublisher.publishEvent(new PartyAssembledEvent(
                party.getId(),
                assembledAt,
                approvedMembersPayload
        ));

        return new PartyDto(party);
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    @Transactional
    public PartyDto complete(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 처리할 수 있습니다.");
        }

        party.complete(); // IN_PROGRESS 아니면 409-1

        // 성취 ACHIEVED 전이, PARTY_PR 동기화 중단은 해당 도메인이 리스너를 붙이면 되므로 여기서는 이벤트 발행까지만
        eventPublisher.publishEvent(new PartyCompletedEvent(party.getId(), party.getCompletedAt()));

        return new PartyDto(party);
    }
}
