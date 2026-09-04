package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PartySearchServiceLogFailureIsolationTest {

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
    private PartySearchService partySearchService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void searchStillSucceedsWhenSearchLogWriteFailsOnForeignKeyViolation() {
        Member owner = memberRepository.save(new Member("log-failure-owner@test.com", "pw", "owner", null));
        Member nonExistentActor = new Member(999_999_999L, Role.MEMBER);

        Party party = partyRepository.save(new Party(
                owner, "파티명", "백엔드 스터디", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "백엔드 스터디"));

        assertThatCode(() ->
                partySearchService.search(nonExistentActor, "백엔드", PageRequest.of(0, 10))
        ).doesNotThrowAnyException();
    }
}
