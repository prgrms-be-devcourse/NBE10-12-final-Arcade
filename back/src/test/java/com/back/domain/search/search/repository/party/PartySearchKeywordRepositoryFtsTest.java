package com.back.domain.search.search.repository.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
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

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PartySearchKeywordRepositoryFtsTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findsPartyByMatchingKeyword() {
        Member owner = memberRepository.save(new Member("fts-owner@test.com", "pw", "owner", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명", "제목", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "백엔드 스터디"));

        Page<Long> result = partySearchKeywordRepository.searchPartyIdsByKeywords(
                "백엔드", "RECRUITING", PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).contains(party.getId());
    }

    @Test
    void doesNotMatchUnrelatedKeyword() {
        Member owner = memberRepository.save(new Member("fts-owner2@test.com", "pw", "owner2", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명2", "제목2", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "프론트엔드 스터디"));

        Page<Long> result = partySearchKeywordRepository.searchPartyIdsByKeywords(
                "게임개발", "RECRUITING", PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).doesNotContain(party.getId());
    }

    @Test
    void acceptsCanonicalTermsWithSpecialCharacters() {
        Member owner = memberRepository.save(new Member("fts-owner3@test.com", "pw", "owner3", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명3", "제목3", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "C++ 스터디"));

        Page<Long> result = partySearchKeywordRepository.searchPartyIdsByKeywords(
                "C++ | C#", "RECRUITING", PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).contains(party.getId());
    }
}
