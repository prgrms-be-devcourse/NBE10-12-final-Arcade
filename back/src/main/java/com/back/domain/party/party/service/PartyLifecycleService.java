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
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 파티 라이프사이클 전이(모집 마감/완료 판정)만 담당. 일반 CRUD는 PartyService.
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

        List<Member> approvedMembers = members.stream()
                .filter(m -> m.getStatus() == PartyMemberStatus.APPROVED)
                .map(PartyMember::getMember)
                .toList();

        // 파티 확정 원본 사건(PartyAssemble)과 그 시점 승인된 참여자별 파생 레코드를 분리 기록
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(party));
        List<PartyAssembleToMember> assembleToMembers = approvedMembers.stream()
                .map(member -> new PartyAssembleToMember(partyAssemble, member))
                .toList();
        partyAssembleToMemberRepository.saveAll(assembleToMembers);

        // 성취(Goal) 자동생성, 체크리스트 오픈은 해당 도메인이 리스너를 붙이면 되므로
        // 여기서는 이벤트 발행까지만 - 동기 호출로 강결합시키지 않는다(기획서 3.6)
        eventPublisher.publishEvent(new PartyAssembledEvent(
                party.getId(),
                approvedMembers.stream().map(Member::getId).toList()
        ));

        return new PartyDto(party);
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }
}
