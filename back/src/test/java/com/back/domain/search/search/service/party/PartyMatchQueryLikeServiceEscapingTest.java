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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartyMatchQueryLikeServiceEscapingTest {

    @Autowired
    private PartyMatchQueryLikeService partyMatchQueryLikeService;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void percentInKeywordIsTreatedAsLiteralCharacterNotWildcard() {
        Member owner = memberRepository.save(new Member("like-escape-owner@test.com", "pw", "owner", null));
        Party literalMatch = partyRepository.save(new Party(
                owner, "파티명1", "제목1", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        Party wildcardOnlyMatch = partyRepository.save(new Party(
                owner, "파티명2", "제목2", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(literalMatch, "a%b 스터디"));
        partySearchKeywordRepository.save(new PartySearchKeyword(wildcardOnlyMatch, "aXXXb 스터디"));

        Page<Long> result = partyMatchQueryLikeService.findMatchingPartyIds(
                List.of("a%b"), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).contains(literalMatch.getId());
        assertThat(result.getContent()).doesNotContain(wildcardOnlyMatch.getId());
    }
}
