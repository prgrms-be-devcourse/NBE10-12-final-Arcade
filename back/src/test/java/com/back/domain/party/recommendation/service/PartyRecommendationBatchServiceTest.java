package com.back.domain.party.recommendation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import com.back.domain.search.search.service.party.PartySearchKeywordPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE: 클래스 레벨 @Transactional 미사용 — PartyRecommendationCandidateServiceTest와 동일한 이유.
 */
@ActiveProfiles("test")
@SpringBootTest
class PartyRecommendationBatchServiceTest {

    @Autowired private PartyRecommendationBatchService batchService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyRecommendationRepository partyRecommendationRepository;
    @Autowired private PartySearchKeywordPort partySearchKeywordPort;

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private String uniqueEmail(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "@test.com";
    }

    private Member newMember(String emailPrefix) {
        String email = uniqueEmail(emailPrefix);
        return memberRepository.save(new Member(email, "pw", email, null));
    }

    private Party newIndexedParty(Member owner, String title) {
        Party party = new Party(
                owner, "테스트팀", title + "-" + SEQ.incrementAndGet(), "설명", null, null, null,
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 3));
        party = partyRepository.save(party);
        partySearchKeywordPort.keywordParty(party.getId());
        return party;
    }

    private MemberProfile newProfile(Member member, PositionType position, List<String> techStacks) {
        return memberProfileRepository.save(new MemberProfile(member, null, null, position, techStacks));
    }

    @Test
    @DisplayName("멤버 프로필 기준으로 추천 결과를 순위와 함께 저장한다")
    void computeForMemberSavesRankedRecommendations() {
        Member owner = newMember("owner");
        newIndexedParty(owner, "백엔드 개발자 모집");

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));

        batchService.computeForMember(actor);

        List<PartyRecommendation> saved = partyRecommendationRepository.findByMemberIdOrderByRank(actor.getId());
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("재계산 시 기존 추천을 새 결과로 교체한다")
    void computeForMemberReplacesExistingRecommendations() {
        Member owner = newMember("owner");
        newIndexedParty(owner, "백엔드 개발자 모집");

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));

        batchService.computeForMember(actor);
        int firstRunCount = partyRecommendationRepository.findByMemberIdOrderByRank(actor.getId()).size();

        batchService.computeForMember(actor);
        int secondRunCount = partyRecommendationRepository.findByMemberIdOrderByRank(actor.getId()).size();

        assertThat(secondRunCount).isEqualTo(firstRunCount);
    }

    @Test
    @DisplayName("프로필이 없는 멤버는 전체 계산에서 건너뛴다")
    void computeAllSkipsMembersWithoutProfile() {
        Member noProfileMember = newMember("noprofile");

        batchService.computeAll();

        List<PartyRecommendation> saved = partyRecommendationRepository.findByMemberIdOrderByRank(noProfileMember.getId());
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("프로필이 있는 모든 멤버에 대해 추천을 계산한다")
    void computeAllProcessesEveryMemberWithProfile() {
        Member owner = newMember("owner");
        newIndexedParty(owner, "백엔드 개발자 모집");

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));

        batchService.computeAll();

        List<PartyRecommendation> saved = partyRecommendationRepository.findByMemberIdOrderByRank(actor.getId());
        assertThat(saved).isNotEmpty();
    }
}
