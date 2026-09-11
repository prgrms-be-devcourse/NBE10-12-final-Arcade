package com.back.domain.party.party.service;

import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartySortOption;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.position.repository.PositionRepository;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PartyServiceAdminTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private LikeActionRepository likeActionRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Test
    void hidingAPartyExcludesItFromPublicList() {
        Member owner = saveMember("adm-hide-owner@test.com");
        PartyDto created = createParty(owner, "숨김 테스트 파티");

        partyService.hide(created.id());

        Page<PartyListItemDto> publicList = partyService.getList(
                null, null, null, PartySortOption.DEADLINE, PageRequest.of(0, 20));

        assertThat(publicList.getContent()).extracting(PartyListItemDto::id)
                .doesNotContain(created.id());
    }

    @Test
    void unhidingBringsItBackToPublicList() {
        Member owner = saveMember("adm-unhide-owner@test.com");
        PartyDto created = createParty(owner, "숨김해제 테스트 파티");
        partyService.hide(created.id());

        partyService.unhide(created.id());

        Page<PartyListItemDto> publicList = partyService.getList(
                null, null, null, PartySortOption.DEADLINE, PageRequest.of(0, 20));

        assertThat(publicList.getContent()).extracting(PartyListItemDto::id)
                .contains(created.id());
    }

    @Test
    void hiddenPartyDetailReturns404ToPublic() {
        Member owner = saveMember("adm-hide-detail-owner@test.com");
        PartyDto created = createParty(owner, "숨김 상세 테스트 파티");
        partyService.hide(created.id());

        assertThatThrownBy(() -> partyService.getDetail(created.id(), true))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("존재하지 않는 파티");
    }

    @Test
    void adminListReturnsHiddenPartiesThatPublicListExcludes() {
        Member owner = saveMember("adm-list-owner@test.com");
        PartyDto created = createParty(owner, "관리자 목록 테스트 파티");
        partyService.hide(created.id());

        Page<PartyListItemDto> adminList = partyService.getListForAdmin(
                null, null, PageRequest.of(0, 20));

        assertThat(adminList.getContent())
                .filteredOn(item -> item.id() == created.id())
                .hasSize(1)
                .allSatisfy(item -> assertThat(item.hidden()).isTrue());
    }

    // 이 케이스가 이 기능의 핵심이다 - 일반 delete()는 승인된 파티원이 있으면 막히지만
    // deleteAsAdmin()은 신고·정책위반 대응이 목적이라 같은 상황에서도 강제로 지울 수 있어야 한다.
    @Test
    void adminCanForceDeletePartyEvenWithApprovedMember() {
        Member owner = saveMember("adm-delete-owner@test.com");
        Member approvedMember = saveMember("adm-delete-member@test.com");
        PartyDto created = createParty(owner, "강제삭제 테스트 파티");
        Party party = partyRepository.findById(created.id()).orElseThrow();
        Position position = positionRepository.findById(created.positions().get(0).id()).orElseThrow();

        PartyMember membership = new PartyMember(party, approvedMember, position, null);
        membership.approve();
        partyMemberRepository.save(membership);

        assertThatThrownBy(() -> partyService.delete(created.id(), owner))
                .isInstanceOf(ServiceException.class);

        partyService.deleteAsAdmin(created.id());

        assertThat(partyRepository.findById(created.id())).isEmpty();
    }

    @Test
    void adminDeleteCleansUpLikesAndBookmarksForTheParty() {
        Member owner = saveMember("adm-delete-like-owner@test.com");
        Member liker = saveMember("adm-delete-like-liker@test.com");
        PartyDto created = createParty(owner, "좋아요 정리 테스트 파티");

        likeActionRepository.save(new LikeAction(liker, TargetType.PARTY, created.id()));
        bookmarkRepository.save(new Bookmark(liker, TargetType.PARTY, created.id()));

        partyService.deleteAsAdmin(created.id());

        assertThat(likeActionRepository.existsByMemberAndTargetTypeAndTargetId(
                liker, TargetType.PARTY, created.id())).isFalse();
        assertThat(bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(
                liker, TargetType.PARTY, created.id())).isFalse();
    }

    @Test
    void deleteAsAdminIsIdempotentWhenPartyAlreadyGone() {
        long nonExistentId = 999_999_999L;

        assertThatCode(() -> partyService.deleteAsAdmin(nonExistentId))
                .doesNotThrowAnyException();
    }

    private Member saveMember(String email) {
        Member member = memberRepository.save(new Member(email, "pw", "닉네임", null));
        memberProfileRepository.save(new MemberProfile(member, null, null, PositionType.BACK, List.of()));
        return member;
    }

    private PartyDto createParty(Member owner, String title) {
        return partyService.create(
                owner, "파티명", title, null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );
    }
}
