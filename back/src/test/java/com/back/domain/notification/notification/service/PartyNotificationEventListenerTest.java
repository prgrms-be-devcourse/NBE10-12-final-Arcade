package com.back.domain.notification.notification.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.event.PartyApplicationApprovedEvent;
import com.back.domain.party.application.event.PartyApplicationReceivedEvent;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.event.PartyAssembledEvent;
import com.back.domain.party.party.event.PartyCompletedEvent;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.event.PartyShowcasePublishedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private Party party;

    @Mock
    private Member owner;

    @Mock
    private Member participant;

    @InjectMocks
    private PartyNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        when(party.getPartyName()).thenReturn("테스트");
    }

    @Test
    @DisplayName("파티 지원 시 파티장에게 신규 지원 알림을 생성한다")
    void received() {
        when(memberRepository.getReferenceById(10L)).thenReturn(owner);

        listener.received(new PartyApplicationReceivedEvent(1L, 10L));

        verify(notificationService).create(owner, NotificationType.PARTY_APPLICATION_RECEIVED,
                "테스트 파티에 새로운 지원자가 있습니다.");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("파티 지원 승인 시 승인된 회원에게 알림을 생성한다")
    void approved() {
        when(memberRepository.getReferenceById(20L)).thenReturn(participant);

        listener.approved(new PartyApplicationApprovedEvent(1L, 20L));

        verify(notificationService).create(participant, NotificationType.PARTY_APPLICATION_APPROVED,
                "테스트 파티 참여 신청이 승인되었습니다.");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("파티 구성 완료 시 이벤트의 승인 회원에게 알림하고 파티장은 중복 제외한다")
    void assembled() {
        prepareOwnerAndMemberReferences();

        listener.assembled(new PartyAssembledEvent(1L, LocalDate.now(), List.of(
                new PartyAssembledEvent.ApprovedMember(10L, 100L, PositionType.BACK),
                new PartyAssembledEvent.ApprovedMember(20L, 200L, PositionType.BACK))));

        verify(notificationService).create(owner, NotificationType.PARTY_ASSEMBLED,
                "테스트 파티 모집이 마감되어 활동이 시작되었습니다.");
        verify(notificationService).create(participant, NotificationType.PARTY_ASSEMBLED,
                "테스트 파티 모집이 마감되어 활동이 시작되었습니다.");
        verifyNoMoreInteractions(notificationService);
        verifyNoInteractions(partyMemberRepository);
    }

    @Test
    @DisplayName("파티 활동 완료 시 파티장과 승인 회원에게만 중복 없이 알림한다")
    void completed() {
        preparePartyMembers();

        listener.completed(new PartyCompletedEvent(1L, LocalDateTime.now()));

        verify(notificationService).create(owner, NotificationType.PARTY_COMPLETED,
                "테스트 파티 활동이 완료되었습니다.");
        verify(notificationService).create(participant, NotificationType.PARTY_COMPLETED,
                "테스트 파티 활동이 완료되었습니다.");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("파티 성과 게시 시 파티장과 승인 회원에게만 중복 없이 알림한다")
    void showcasePublished() {
        preparePartyMembers();

        listener.showcasePublished(new PartyShowcasePublishedEvent(1L, "성과", "설명"));

        verify(notificationService).create(owner, NotificationType.PARTY_SHOWCASE_PUBLISHED,
                "테스트 파티의 성과가 전시관에 게시되었습니다.");
        verify(notificationService).create(participant, NotificationType.PARTY_SHOWCASE_PUBLISHED,
                "테스트 파티의 성과가 전시관에 게시되었습니다.");
        verifyNoMoreInteractions(notificationService);
    }

    private void preparePartyMembers() {
        prepareOwnerAndMemberReferences();
        when(participant.getId()).thenReturn(20L);
        PartyMember ownerMember = partyMember(owner, PartyMemberStatus.APPROVED);
        PartyMember approvedMember = partyMember(participant, PartyMemberStatus.APPROVED);
        PartyMember pendingMember = partyMember(null, PartyMemberStatus.PENDING);
        PartyMember rejectedMember = partyMember(null, PartyMemberStatus.REJECTED);
        when(partyMemberRepository.findAllByParty(party)).thenReturn(
                List.of(ownerMember, approvedMember, pendingMember, rejectedMember));
    }

    private void prepareOwnerAndMemberReferences() {
        when(party.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(10L);
        when(memberRepository.getReferenceById(10L)).thenReturn(owner);
        when(memberRepository.getReferenceById(20L)).thenReturn(participant);
    }

    private PartyMember partyMember(Member member, PartyMemberStatus status) {
        PartyMember partyMember = mock(PartyMember.class);
        when(partyMember.getStatus()).thenReturn(status);
        if (status == PartyMemberStatus.APPROVED) {
            when(partyMember.getMember()).thenReturn(member);
        }
        return partyMember;
    }
}
