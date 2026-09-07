package com.back.domain.party.application.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.dtos.PartyApplicationDto;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.event.PartyApplicationApprovedEvent;
import com.back.domain.party.application.event.PartyApplicationReceivedEvent;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyApplicationService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

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
        partyMemberRepository.save(partyMember);
        eventPublisher.publishEvent(new PartyApplicationReceivedEvent(party.getId(), party.getOwner().getId()));
        return new PartyApplicationDto(partyMember);
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

    // 요청으로 받을 수 있는 값을 승인/거절 둘로만 제한하기 위한 전용 enum.
    // PartyMemberStatus를 그대로 쓰면 클라이언트가 PENDING도 요청값으로 보낼 수 있게 되는데,
    // "지원 상태를 PENDING으로 바꿔달라"는 요청 자체가 의미가 없어서 API 계약에서부터 차단한다.
    public enum Decision {
        APPROVED,
        REJECTED
    }

    @Transactional
    public PartyApplicationDto decide(long partyId, long applicationId, Member actor, Decision decision) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 처리할 수 있습니다.");
        }

        PartyMember partyMember = partyMemberRepository.findByIdAndParty(applicationId, party)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 지원 내역입니다."));

        try {
            if (decision == Decision.APPROVED) {
                partyMember.approve(); // 이미 처리된 지원건이면 409-1
                partyMember.getPosition().fillOneSeat(); // 정원 마감이면 409-2
            } else {
                partyMember.reject();
            }
            // @Version 충돌은 커밋 시점(더티체킹)에야 감지되는데, 그건 이 메서드가 끝나고
            // 트랜잭션 프록시가 반환된 뒤라 여기 catch로 못 잡는다. flush()로 지금 이 시점에
            // 강제로 UPDATE를 내보내서, 충돌이면 바로 여기서 예외가 터지게 만든다.
            entityManager.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            // 승인 시점에 다른 요청과 @Version 충돌 - Position.fillOneSeat()의 자체 정원
            // 체크와는 별개로, DB 레벨 낙관적 락 자체가 깨진 경우도 동일하게 409-2로 응답
            throw new ServiceException("409-2", "정원이 마감되어 승인할 수 없습니다. 새로고침 후 다시 시도해주세요.");
        }

        if (decision == Decision.APPROVED) {
            eventPublisher.publishEvent(new PartyApplicationApprovedEvent(party.getId(), partyMember.getMember().getId()));
        }
        return new PartyApplicationDto(partyMember);
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    // 이미 승인된 파티원의 승인을 취소한다
    // 파티 삭제는 승인된 파티원이 하나도 없어야 가능해서 삭제 전에 파티장이 승인된 사람들을 이걸로 하나씩 정리해야 한다.
    @Transactional
    public PartyApplicationDto cancelApproval(long partyId, long applicationId, Member actor) {
        Party party = findPartyOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 처리할 수 있습니다.");
        }

        PartyMember partyMember = partyMemberRepository.findByIdAndParty(applicationId, party)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 지원 내역입니다."));

        partyMember.cancelApproval();
        partyMember.getPosition().freeOneSeat();

        return new PartyApplicationDto(partyMember);
    }
}
