package com.back.domain.party.party.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * party_name/title LIKE 검색에서 keyword가 null일 때 Postgres가
 * "operator does not exist: character varying ~~ bytea"로 죽던 버그의 회귀 테스트.
 * H2는 이 오류를 재현하지 않아 반드시 실제 Postgres(Testcontainers)로 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PartySearchNullKeywordPostgresTest {

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
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void searchWithNullKeywordDoesNotThrowOnRealPostgres() {
        assertThatCode(() -> partyRepository.search(null, null, null, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void searchWithKeywordStillMatchesOnRealPostgres() {
        Member owner = memberRepository.save(new Member("null-keyword-owner@test.com", "pw", "owner", null));
        Party party = partyRepository.save(new Party(
                owner, "백엔드 스터디", "제목", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));

        Page<Party> result = partyRepository.search("백엔드", null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Party::getId).contains(party.getId());
    }

    @Test
    void searchOrderByVacancyWithNullKeywordDoesNotThrowOnRealPostgres() {
        assertThatCode(() ->
                partyRepository.searchOrderByVacancy(null, PartyTag.WEB, PositionType.BACK, PageRequest.of(0, 20))
        ).doesNotThrowAnyException();
    }
}
