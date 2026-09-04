package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartySearchKeywordServiceIdempotencyTest {

    @Autowired
    private PartySearchKeywordPort partySearchKeywordPort;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void staleTriggerAfterTitleChangeStillConvergesToCurrentTitle() {
        Member owner = memberRepository.save(new Member("ordering-owner@test.com", "pw", "owner", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명", "제목A", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordPort.keywordParty(party.getId());

        party.update(
                "파티명", "제목B", null, null, null, null,
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        );
        partyRepository.save(party);

        partySearchKeywordPort.keywordParty(party.getId());

        String keywords = partySearchKeywordRepository.findByParty_Id(party.getId()).orElseThrow().getKeywords();
        assertThat(keywords).contains("b");
        assertThat(keywords).doesNotContain("a");
    }
}
