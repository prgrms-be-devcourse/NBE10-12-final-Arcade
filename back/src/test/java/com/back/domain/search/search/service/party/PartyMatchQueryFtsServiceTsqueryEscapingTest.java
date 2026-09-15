package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.back.support.PostgresTestProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
@SpringBootTest
@Testcontainers
class PartyMatchQueryFtsServiceTsqueryEscapingTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.registerForProdProfile(registry, POSTGRES);
    }

    @Autowired
    private PartyMatchQueryFtsService partyMatchQueryFtsService;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Party createParty(String suffix, String keywords) {
        return createParty(suffix, keywords, PartyTag.WEB);
    }

    private Party createParty(String suffix, String keywords, PartyTag partyTag) {
        return createParty(suffix, keywords, TopicType.STUDY, partyTag);
    }

    private Party createParty(String suffix, String keywords, TopicType topicType, PartyTag partyTag) {
        Member owner = memberRepository.save(new Member("fts-escape-" + suffix + "@test.com", "pw", "owner" + suffix, null));
        Party party = new Party(
                owner, "파티명" + suffix, "제목" + suffix, null, null, "외부 대회", "https://example.com",
                topicType, partyTag, null, LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));
        party = partyRepository.save(party);
        partySearchKeywordRepository.save(new PartySearchKeyword(party, keywords));
        return party;
    }

    @Test
    void doesNotThrowOnKeywordsContainingTsqueryOperatorCharacters() {
        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("<script>", "a b", "a:b", "&", "|", "foo\\", "foo\\bar"), null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void stillMatchesNormalKeywordsAfterEscaping() {
        Party party = createParty("normal", "백엔드 스터디");

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("백엔드"), null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).contains(party.getId());
    }

    @Test
    void doesNotSubstringMatchUnrelatedLongerLexeme() {
        Party party = createParty("substring", "자바스크립트 스터디");

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("자바"), null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).doesNotContain(party.getId());
    }

    @Test
    void filtersByPartyTagOnRealPostgres() {
        Party webParty = createParty("tag-web", "백엔드 스터디", PartyTag.WEB);
        Party appParty = createParty("tag-app", "백엔드 스터디", PartyTag.APP);

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("백엔드"), PartyTag.APP, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .contains(appParty.getId())
                .doesNotContain(webParty.getId());
    }

    @Test
    void filtersByTopicTypeOnRealPostgres() {
        Party studyParty = createParty("topic-study", "백엔드 스터디", TopicType.STUDY, PartyTag.WEB);
        Party contestParty = createParty("topic-contest", "백엔드 스터디", TopicType.CONTEST, PartyTag.WEB);

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("백엔드"), null, TopicType.CONTEST, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .contains(contestParty.getId())
                .doesNotContain(studyParty.getId());
    }

    @Test
    void filtersByPositionTypeOnRealPostgres() {
        Member owner = memberRepository.save(new Member("fts-position-owner@test.com", "pw", "owner", null));
        Party backParty = new Party(
                owner, "파티명-back", "제목-back", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        );
        backParty.addPosition(new Position(PositionType.BACK, 2));
        backParty = partyRepository.save(backParty);
        partySearchKeywordRepository.save(new PartySearchKeyword(backParty, "백엔드 스터디"));

        Party frontParty = new Party(
                owner, "파티명-front", "제목-front", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        );
        frontParty.addPosition(new Position(PositionType.FRONT, 2));
        frontParty = partyRepository.save(frontParty);
        partySearchKeywordRepository.save(new PartySearchKeyword(frontParty, "백엔드 스터디"));

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("백엔드"), null, null, PositionType.FRONT, PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .contains(frontParty.getId())
                .doesNotContain(backParty.getId());
    }
}
