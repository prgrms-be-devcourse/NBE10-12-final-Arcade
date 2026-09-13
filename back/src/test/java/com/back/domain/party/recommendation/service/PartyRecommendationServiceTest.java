package com.back.domain.party.recommendation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.party.service.PartyService;
import com.back.domain.party.recommendation.dtos.PartyRecommendationResultDto;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PartyRecommendationServiceTest {

    @Autowired
    private PartyRecommendationService partyRecommendationService;

    @Autowired
    private PartyRecommendationRepository partyRecommendationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyService partyService;

    @Test
    @DisplayName("프로필이 없는 회원은 profileRequired=true, 목록은 비어있다")
    void getRecommendations_withoutProfile_returnsProfileRequired() {
        Member member = memberRepository.save(new Member("rec-no-profile@test.com", "pw", "닉네임", null));

        PartyRecommendationResultDto result = partyRecommendationService.getRecommendations(member);

        assertThat(result.profileRequired()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("프로필이 있고 추천 결과가 있으면 rank 순서대로 내려온다")
    void getRecommendations_withProfile_returnsItemsInRankOrder() {
        Member member = saveMemberWithProfile("rec-with-profile@test.com");
        Member owner = saveMemberWithProfile("rec-party-owner@test.com");

        PartyDto party1 = createParty(owner, "추천 테스트 파티1");
        PartyDto party2 = createParty(owner, "추천 테스트 파티2");

        partyRecommendationRepository.save(new PartyRecommendation(member.getId(), party2.id(), 1, null));
        partyRecommendationRepository.save(new PartyRecommendation(member.getId(), party1.id(), 2, null));

        PartyRecommendationResultDto result = partyRecommendationService.getRecommendations(member);

        assertThat(result.profileRequired()).isFalse();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).party().id()).isEqualTo(party2.id());
        assertThat(result.items().get(1).party().id()).isEqualTo(party1.id());
    }

    @Test
    @DisplayName("배치 계산 이후 삭제된 파티는 결과에서 빠진다")
    void getRecommendations_skipsDeletedParty() {
        Member member = saveMemberWithProfile("rec-deleted-party@test.com");
        Member owner = saveMemberWithProfile("rec-deleted-owner@test.com");
        PartyDto party = createParty(owner, "삭제될 파티");

        partyRecommendationRepository.save(new PartyRecommendation(member.getId(), party.id(), 1, null));
        partyRepository.deleteById(party.id());

        PartyRecommendationResultDto result = partyRecommendationService.getRecommendations(member);

        assertThat(result.items()).isEmpty();
    }

    private Member saveMemberWithProfile(String email) {
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
