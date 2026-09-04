package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("custom.notification.redis-pubsub.enabled", () -> "false");
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
        Member owner = memberRepository.save(new Member("fts-escape-" + suffix + "@test.com", "pw", "owner" + suffix, null));
        Party party = partyRepository.save(new Party(
                owner, "파티명" + suffix, "제목" + suffix, null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, keywords));
        return party;
    }

    @Test
    void doesNotThrowOnKeywordsContainingTsqueryOperatorCharacters() {
        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("<script>", "a b", "a:b", "&", "|", "foo\\", "foo\\bar"), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void stillMatchesNormalKeywordsAfterEscaping() {
        Party party = createParty("normal", "백엔드 스터디");

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("백엔드"), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).contains(party.getId());
    }

    @Test
    void doesNotSubstringMatchUnrelatedLongerLexeme() {
        Party party = createParty("substring", "자바스크립트 스터디");

        Page<Long> result = partyMatchQueryFtsService.findMatchingPartyIds(
                List.of("자바"), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).doesNotContain(party.getId());
    }
}
