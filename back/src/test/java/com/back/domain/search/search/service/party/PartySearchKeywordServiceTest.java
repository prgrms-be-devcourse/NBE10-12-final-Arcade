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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartySearchKeywordServiceTest {

    @Autowired
    private PartySearchKeywordPort partySearchKeywordPort;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void indexingNormalizesKeywordsBeforeStoring() {
        Member owner = memberRepository.save(
                new Member("owner@test.com", "pw", "owner", null)
        );
        Party party = partyRepository.save(new Party(
                owner, "파티명", "backend 개발자 구합니다", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));

        partySearchKeywordPort.keywordParty(party.getId());

        PartySearchKeyword keyword = partySearchKeywordRepository.findByParty_Id(party.getId()).orElseThrow();

        assertThat(keyword.getKeywords()).contains("백엔드");
        assertThat(keyword.getKeywords()).doesNotContain("backend");
    }
}
