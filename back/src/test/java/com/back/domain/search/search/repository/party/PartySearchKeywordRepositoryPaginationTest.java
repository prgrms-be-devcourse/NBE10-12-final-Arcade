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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PartySearchKeywordRepositoryPaginationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void pagesDoNotOverlapOrDuplicateAcrossRequests() {
        Member owner = memberRepository.save(new Member("fts-pagination-owner@test.com", "pw", "owner", null));
        for (int i = 0; i < 5; i++) {
            Party party = partyRepository.save(new Party(
                    owner, "파티명" + i, "제목" + i, null, null, "외부 대회", "https://example.com",
                    TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
            ));
            partySearchKeywordRepository.save(new PartySearchKeyword(party, "에프티에스페이징 스터디"));
        }

        Page<Party> page0 = partySearchKeywordRepository.searchByKeywords("에프티에스페이징", "RECRUITING", PageRequest.of(0, 2));
        Page<Party> page1 = partySearchKeywordRepository.searchByKeywords("에프티에스페이징", "RECRUITING", PageRequest.of(1, 2));
        Page<Party> page2 = partySearchKeywordRepository.searchByKeywords("에프티에스페이징", "RECRUITING", PageRequest.of(2, 2));

        List<Long> allIds = List.of(page0, page1, page2).stream()
                .flatMap(p -> p.getContent().stream())
                .map(Party::getId)
                .collect(Collectors.toList());

        assertThat(allIds).hasSize(5);
        assertThat(allIds).doesNotHaveDuplicates();
    }
}
