package com.back.domain.search.search.controller;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Test
    void findsPartyBySynonymAndReturnsExtractedKeywords() throws Exception {
        Member owner = memberRepository.save(new Member("search-controller-owner@test.com", "pw", "owner", null));
        Party party = partyRepository.save(new Party(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(party, "백엔드 스터디"));

        mvc.perform(get("/api/v1/parties/search").param("q", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.matchedKeywords").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(party.getId()));
    }

    @Test
    void partyTagFiltersOutOtherTags() throws Exception {
        Member owner = memberRepository.save(new Member("search-controller-tag-owner@test.com", "pw", "owner", null));
        Party webParty = partyRepository.save(new Party(
                owner, "파티명W", "백엔드 스터디W", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        ));
        Party appParty = partyRepository.save(new Party(
                owner, "파티명A", "백엔드 스터디A", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.APP, null, LocalDateTime.now().plusDays(7)
        ));
        partySearchKeywordRepository.save(new PartySearchKeyword(webParty, "백엔드 스터디"));
        partySearchKeywordRepository.save(new PartySearchKeyword(appParty, "백엔드 스터디"));

        mvc.perform(get("/api/v1/parties/search").param("q", "백엔드").param("partyTag", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(appParty.getId()))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void missingQReturns400_1() throws Exception {
        mvc.perform(get("/api/v1/parties/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    void punctuationOnlyQReturns400_4() throws Exception {
        mvc.perform(get("/api/v1/parties/search").param("q", "!!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    void tooLongQReturns400_1() throws Exception {
        mvc.perform(get("/api/v1/parties/search").param("q", "가".repeat(26)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }
}
