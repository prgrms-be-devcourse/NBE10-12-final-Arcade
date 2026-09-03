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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartyMatchQueryLikeServicePaginationTest {

    @Autowired
    private PartyMatchQueryLikeService partyMatchQueryLikeService;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void pagesDoNotOverlapOrDuplicateAcrossRequests() {
        Member owner = memberRepository.save(new Member("pagination-owner@test.com", "pw", "owner", null));
        for (int i = 0; i < 5; i++) {
            Party party = partyRepository.save(new Party(
                    owner, "파티명" + i, "제목" + i, null, null, "외부 대회", "https://example.com",
                    TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
            ));
            partySearchKeywordRepository.save(new PartySearchKeyword(party, "페이징테스트 스터디"));
        }

        List<String> keywords = List.of("페이징테스트");

        Page<Party> page0 = partyMatchQueryLikeService.findMatchingParties(keywords, PageRequest.of(0, 2));
        Page<Party> page1 = partyMatchQueryLikeService.findMatchingParties(keywords, PageRequest.of(1, 2));
        Page<Party> page2 = partyMatchQueryLikeService.findMatchingParties(keywords, PageRequest.of(2, 2));

        List<Long> allIds = List.of(page0, page1, page2).stream()
                .flatMap(p -> p.getContent().stream())
                .map(Party::getId)
                .collect(Collectors.toList());

        assertThat(allIds).hasSize(5);
        assertThat(allIds).doesNotHaveDuplicates();
    }
}
