package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class PartyMatchQueryLikeServiceNPlusOneTest {

    @Autowired
    private PartyMatchQueryLikeService partyMatchQueryLikeService;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void doesNotIssueOneQueryPerPartyWhenFetchingMatches() {
        Member owner = memberRepository.save(new Member("nplus1-owner@test.com", "pw", "owner", null));
        for (int i = 0; i < 5; i++) {
            Party party = partyRepository.save(new Party(
                    owner, "파티명" + i, "제목" + i, null, null, "외부 대회", "https://example.com",
                    TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
            ));
            partySearchKeywordRepository.save(new PartySearchKeyword(party, "엔플러스원테스트 스터디"));
        }

        Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Party> result = partyMatchQueryLikeService.findMatchingParties(
                java.util.List.of("엔플러스원테스트"), PageRequest.of(0, 10)
        );
        result.getContent().forEach(Party::getId);

        assertThat(result.getContent()).hasSize(5);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
