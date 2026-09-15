package com.back.domain.party.recommendation.service;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.search.search.entity.SearchLog;
import com.back.domain.search.search.repository.SearchLogRepository;
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
 * NOTE: 클래스 레벨에 @Transactional을 붙이지 않습니다.
 * PartySearchKeywordService.keywordParty()가 REQUIRES_NEW로 동작하기 때문에,
 * 바깥에 @Transactional 테스트 트랜잭션이 걸려 있으면 그 안에서 저장한 Party가
 * 아직 커밋되지 않아 keywordParty() 쪽 새 트랜잭션에서는 보이지 않아
 * NoSuchElementException이 발생합니다.
 */
@ActiveProfiles("test")
@SpringBootTest
class PartyRecommendationCandidateServiceTest {

    @Autowired private PartyRecommendationCandidateService candidateService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyMemberRepository partyMemberRepository;
    @Autowired private SearchLogRepository searchLogRepository;
    @Autowired private GoalRepository goalRepository;
    @Autowired private PartySearchKeywordPort partySearchKeywordPort;

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private String uniqueEmail(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "@test.com";
    }

    private Member newMember(String emailPrefix) {
        String email = uniqueEmail(emailPrefix);
        return memberRepository.save(new Member(email, "pw", email, null));
    }

    private Party newIndexedParty(Member owner, String title, PositionType positionType) {
        Party party = new Party(
                owner, "테스트팀", title + "-" + SEQ.incrementAndGet(), "설명", null, null, null,
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(positionType, 3));
        party = partyRepository.save(party);
        partySearchKeywordPort.keywordParty(party.getId());
        return party;
    }

    private MemberProfile newProfile(Member member, PositionType position, List<String> techStacks) {
        return memberProfileRepository.save(new MemberProfile(member, null, null, position, techStacks));
    }

    @Test
    @DisplayName("포지션과 기술 스택이 맞는 파티를 후보로 반환한다")
    void matchesByPositionAndTechStack() {
        Member owner = newMember("owner");
        Party party = newIndexedParty(owner, "백엔드 개발자 모집", PositionType.BACK);

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));

        List<Long> result = candidateService.selectCandidatePartyIds(actor, 10);

        assertThat(result).contains(party.getId());
    }

    @Test
    @DisplayName("본인이 만든 파티는 후보에서 제외한다")
    void excludesOwnParty() {
        Member owner = newMember("owner");
        Party party = newIndexedParty(owner, "백엔드 개발자 모집", PositionType.BACK);
        newProfile(owner, PositionType.BACK, List.of("백엔드"));

        List<Long> result = candidateService.selectCandidatePartyIds(owner, 10);

        assertThat(result).doesNotContain(party.getId());
    }

    @Test
    @DisplayName("이미 지원한 파티는 후보에서 제외한다")
    void excludesAlreadyAppliedParty() {
        Member owner = newMember("owner");
        Party party = newIndexedParty(owner, "백엔드 개발자 모집", PositionType.BACK);

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));
        partyMemberRepository.save(new PartyMember(party, actor, party.getPositions().get(0), "지원합니다"));

        List<Long> result = candidateService.selectCandidatePartyIds(actor, 10);

        assertThat(result).doesNotContain(party.getId());
    }

    @Test
    @DisplayName("검색 로그와 목표 키워드를 기반으로도 후보를 매칭한다")
    void matchesBySearchLogAndGoalKeywords() {
        Member owner = newMember("owner");
        Party party = newIndexedParty(owner, "리액트 스터디 모집", PositionType.FRONT);

        Member actor = newMember("actor");
        newProfile(actor, null, List.of());
        searchLogRepository.save(new SearchLog(actor, "리액트"));
        goalRepository.save(new PersonalChecklist(actor, GoalStatus.IN_PROGRESS, "리액트 마스터하기", null, null));

        List<Long> result = candidateService.selectCandidatePartyIds(actor, 10);

        assertThat(result).contains(party.getId());
    }

    @Test
    @DisplayName("매칭 키워드가 전혀 없으면 빈 목록을 반환한다")
    void returnsEmptyWhenNoKeywords() {
        Member actor = newMember("actor");
        newProfile(actor, null, List.of());

        List<Long> result = candidateService.selectCandidatePartyIds(actor, 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("limit 개수만큼만 잘라서 반환한다")
    void truncatesToLimit() {
        Member owner = newMember("owner");
        for (int i = 0; i < 5; i++) {
            newIndexedParty(owner, "백엔드 개발자 모집 " + i, PositionType.BACK);
        }

        Member actor = newMember("actor");
        newProfile(actor, PositionType.BACK, List.of("백엔드"));

        List<Long> result = candidateService.selectCandidatePartyIds(actor, 3);

        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }
}
