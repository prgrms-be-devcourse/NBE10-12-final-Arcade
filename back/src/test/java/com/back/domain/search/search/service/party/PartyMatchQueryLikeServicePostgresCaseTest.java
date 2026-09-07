package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import com.back.domain.search.search.service.keyword.KeywordExtractionPort;
import com.back.domain.search.search.service.keyword.KeywordNormalizationPort;
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

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PartyMatchQueryLikeServicePostgresCaseTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.register(registry, POSTGRES);
    }

    @Autowired
    private PartyMatchQueryLikeService partyMatchQueryLikeService;

    @Autowired
    private KeywordExtractionPort keywordExtractionPort;

    @Autowired
    private KeywordNormalizationPort keywordNormalizationPort;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void aiSynonymQueryMatchesLowercaseStoredKeywordOnRealPostgres() {
        Member owner = memberRepository.save(new Member("like-case-owner@test.com", "pw", "owner", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명", "인공지능 스터디", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "ai 스터디"));

        List<String> normalized = keywordNormalizationPort.normalize(keywordExtractionPort.extract("인공지능"));

        Page<Long> result = partyMatchQueryLikeService.findMatchingPartyIds(normalized, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).contains(party.getId());
    }

    @Test
    void filtersByPartyTagOnRealPostgres() {
        Member owner = memberRepository.save(new Member("like-tag-owner@test.com", "pw", "owner", null));
        Party webParty = partyRepository.save(new Party(
                owner, "파티명W", "백엔드 스터디W", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        Party appParty = partyRepository.save(new Party(
                owner, "파티명A", "백엔드 스터디A", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.APP, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(webParty, "백엔드 스터디"));
        partySearchKeywordRepository.save(new PartySearchKeyword(appParty, "백엔드 스터디"));

        Page<Long> result = partyMatchQueryLikeService.findMatchingPartyIds(
                List.of("백엔드"), PartyTag.APP.name(), null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .contains(appParty.getId())
                .doesNotContain(webParty.getId());
    }
}
