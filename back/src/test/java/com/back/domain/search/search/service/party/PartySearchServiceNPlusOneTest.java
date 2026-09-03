package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import jakarta.persistence.EntityManager;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class PartySearchServiceNPlusOneTest {

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

    @Autowired
    private EntityManager entityManager;

    @Test
    void doesNotIssueOneQueryPerOwnerWhenMappingToDto() {
        for (int i = 0; i < 3; i++) {
            Member owner = memberRepository.save(new Member("owner-nplus1-" + i + "@test.com", "pw", "owner" + i, null));
            Party party = partyRepository.save(new Party(
                    owner, "파티명" + i, "제목" + i, null, null, "외부 대회", "https://example.com",
                    TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
            ));
            partySearchKeywordRepository.save(new PartySearchKeyword(party, "오너엔플러스원테스트 스터디"));
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Long> matchedIds = partyMatchQueryLikeService.findMatchingPartyIds(
                List.of("오너엔플러스원테스트"), PageRequest.of(0, 10)
        );
        List<Party> parties = partyRepository.findAllByIdIn(matchedIds.getContent());
        List<PartyListItemDto> dtos = parties.stream().map(PartyListItemDto::new).toList();

        assertThat(dtos).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }
}
