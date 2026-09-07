package com.back.domain.notification.notification.service;

import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.event.PartyApplicationApprovedEvent;
import com.back.domain.party.application.event.PartyApplicationReceivedEvent;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.event.PartyAssembledEvent;
import com.back.domain.party.party.event.PartyCompletedEvent;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.event.PartyShowcasePublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PartyNotificationEventListener {
    private final NotificationService notificationService;
    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;
    private final PartyMemberRepository partyMemberRepository;

    // 원본 작업과 알림을 함께 커밋한다. SSE 전송은 NotificationCreatedEvent의 AFTER_COMMIT 리스너가 담당한다.
    @EventListener
    public void received(PartyApplicationReceivedEvent event) {
        notify(findParty(event.partyId()), event.memberId(), NotificationType.PARTY_APPLICATION_RECEIVED,
                " 파티에 새로운 지원자가 있습니다.");
    }

    @EventListener
    public void approved(PartyApplicationApprovedEvent event) {
        notify(findParty(event.partyId()), event.memberId(), NotificationType.PARTY_APPLICATION_APPROVED,
                " 파티 참여 신청이 승인되었습니다.");
    }

    @EventListener
    public void assembled(PartyAssembledEvent event) {
        Party party = findParty(event.partyId());
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(party.getOwner().getId());

        event.approvedMembers()
                .forEach(
                        member -> recipients.add(member.memberId()));
        recipients.forEach(id -> notify(party, id, NotificationType.PARTY_ASSEMBLED,
                " 파티 모집이 마감되어 활동이 시작되었습니다."));
    }

    @EventListener
    public void completed(PartyCompletedEvent event) {
        notifyParticipants(
                findParty(event.partyId()),
                NotificationType.PARTY_COMPLETED,
                " 파티 활동이 완료되었습니다.");
    }

    @EventListener
    public void showcasePublished(PartyShowcasePublishedEvent event) {
        notifyParticipants(
                findParty(event.partyId()),
                NotificationType.PARTY_SHOWCASE_PUBLISHED,
                " 파티의 성과가 전시관에 게시되었습니다.");
    }

    private void notifyParticipants(Party party, NotificationType type, String message) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(party.getOwner().getId());
        partyMemberRepository.findAllByParty(party).stream()
                .filter(member -> member.getStatus() == PartyMemberStatus.APPROVED)
                .forEach(member -> recipients.add(member.getMember().getId()));
        recipients.forEach(id -> notify(party, id, type, message));
    }

    private void notify(Party party, long memberId, NotificationType type, String message) {
        notificationService.create(
                memberRepository.getReferenceById(memberId),
                type,
                party.getPartyName() + message);
    }

    private Party findParty(long partyId) {
        return partyRepository.findById(partyId).orElseThrow();
    }
}
