package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.repository.PartyMemberRepository;
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
import java.util.Map;

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
    private PartyMemberRepository partyMemberRepository;

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
                    TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
            ));
            partySearchKeywordRepository.save(new PartySearchKeyword(party, "오너엔플러스원테스트 스터디"));
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Long> matchedIds = partyMatchQueryLikeService.findMatchingPartyIds(
                List.of("오너엔플러스원테스트"), null, null, null, PageRequest.of(0, 10)
        );
        List<Party> parties = partyRepository.findAllByIdIn(matchedIds.getContent());
        // 지원자 수도 파티마다 세지 않고 한 번에 집계한다 - 카드 수와 무관하게 쿼리 1방이다
        Map<Long, Long> applicantCounts = partyMemberRepository.countApplicantsByPartyIds(
                matchedIds.getContent());
        List<PartyListItemDto> dtos = parties.stream()
                .map(party -> new PartyListItemDto(party, applicantCounts.getOrDefault(party.getId(), 0L)))
                .toList();

        assertThat(dtos).hasSize(3);
        // 키워드 매칭 + 파티 조회(소유자 join fetch) + 지원자 집계 + 포지션 배치 = 4방.
        // 파티가 3개든 300개든 이 수는 변하지 않는다 - 카드마다 쿼리를 내지 않는다는 게 이 테스트의 요점이다.
        // 대회(targetContest)는 이 테스트 파티들이 전부 미연동이라 조회 자체가 없고,
        // 연동된 파티가 섞여도 Contest 의 @BatchSize 로 1방에 묶인다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }
}
