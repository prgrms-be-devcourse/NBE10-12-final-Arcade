package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
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
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyLifecycleService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ActivityLogService activityLogService;
    private final PartyAssembleRepository partyAssembleRepository;
    private final PartyAssembleToMemberRepository partyAssembleToMemberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PartyDto closeRecruiting(long partyId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 처리할 수 있습니다.");
        }

        party.closeRecruiting(); // RECRUITING 아니면 409-1
        activityLogService.record(actor);

        List<PartyMember> members = partyMemberRepository.findAllByParty(party);

        // 그 시점까지 판정 안 된 지원 건은 일괄 거절
        members.stream()
                .filter(m -> m.getStatus() == PartyMemberStatus.PENDING)
                .forEach(PartyMember::reject);

        // 파티장은 지원 절차(PartyMember)를 거치지 않으므로 이미 들고 있는 party.owner를 확정 명단 맨 앞에 바로 붙인다.
        // 지원한 자리가 없어 포지션은 프로필의 대표 포지션에서 가져온다(Member에는 포지션이 없다).
        List<Confirmed> confirmed = Stream.concat(
                Stream.of(new Confirmed(party.getOwner(), ownerPositionType(party.getOwner()))),
                members.stream()
                        .filter(m -> m.getStatus() == PartyMemberStatus.APPROVED)
                        .map(m -> new Confirmed(m.getMember(), m.getPosition().getType()))
        ).toList();

        // 파티 확정 원본 사건과 그 시점 확정된 참여자별 파생 레코드를 분리 기록
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(party));
        List<PartyAssembleToMember> assembleToMembers = confirmed.stream()
                .map(c -> new PartyAssembleToMember(partyAssemble, c.member()))
                .toList();
        partyAssembleToMemberRepository.saveAll(assembleToMembers);

        LocalDate assembledAt = LocalDate.now();

        // confirmed와 assembleToMembers는 같은 순서로 만들어졌으니 인덱스로 짝지어 memberId + 그 사람의 PartyAssembleToMember id + positionType을 한 번에 이벤트에 실어 보낸다
        List<PartyAssembledEvent.ApprovedMember> approvedMembersPayload = IntStream.range(0, confirmed.size())
                .mapToObj(i -> new PartyAssembledEvent.ApprovedMember(
                        confirmed.get(i).member().getId(),
                        assembleToMembers.get(i).getId(),
                        confirmed.get(i).positionType()
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

    private PositionType ownerPositionType(Member owner) {
        return memberProfileRepository.findByMember(owner)
                .map(MemberProfile::getPosition)
                .orElse(null);
    }

    /** 확정 명단 한 줄. 파티장은 PartyMember가 없어 (회원, 포지션) 쌍으로만 지원자와 나란히 다룬다. */
    private record Confirmed(Member member, PositionType positionType) { }

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
        activityLogService.record(actor);

        // 성취 ACHIEVED 전이, PARTY_PR 동기화 중단은 해당 도메인이 리스너를 붙이면 되므로 여기서는 이벤트 발행까지만
        eventPublisher.publishEvent(new PartyCompletedEvent(party.getId(), party.getCompletedAt()));

        return new PartyDto(party);
    }
}
