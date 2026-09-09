package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.party.service.PartyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdminPartyControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyService partyService;

    @Test
    @DisplayName("관리자 파티 목록: 관리자가 조회하면 200이다")
    @WithUserDetails("admin")
    void listByAdmin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/adm/parties"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("관리자 파티 목록: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void listByNonAdmin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/adm/parties"));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("관리자 파티 목록: 로그인하지 않았으면 401-1이다")
    void listWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/adm/parties"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("파티 숨김: 관리자가 요청하면 hidden이 true가 되고, 일반 목록에서 사라진다")
    @WithUserDetails("admin")
    void hideByAdmin() throws Exception {
        Member owner = saveMember("adm-ctrl-hide-owner@test.com");
        PartyDto created = createParty(owner, "컨트롤러 숨김 테스트 파티");

        mvc.perform(patch("/api/v1/adm/parties/" + created.id() + "/hidden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        assertThat(partyRepository.findById(created.id())).hasValueSatisfying(
                party -> assertThat(party.isHidden()).isTrue());
    }

    @Test
    @DisplayName("파티 숨김: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void hideByNonAdmin() throws Exception {
        Member owner = saveMember("adm-ctrl-hide-nonadmin-owner@test.com");
        PartyDto created = createParty(owner, "컨트롤러 숨김 권한 테스트 파티");

        mvc.perform(patch("/api/v1/adm/parties/" + created.id() + "/hidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(partyRepository.findById(created.id())).hasValueSatisfying(
                party -> assertThat(party.isHidden()).isFalse());
    }

    @Test
    @DisplayName("파티 강제 삭제: 관리자가 요청하면 204-1이고 실제로 삭제된다")
    @WithUserDetails("admin")
    void deleteByAdmin() throws Exception {
        Member owner = saveMember("adm-ctrl-delete-owner@test.com");
        PartyDto created = createParty(owner, "컨트롤러 강제삭제 테스트 파티");

        mvc.perform(delete("/api/v1/adm/parties/" + created.id()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"));

        assertThat(partyRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("파티 강제 삭제: 관리자가 아니면 403-1이고 삭제되지 않는다")
    @WithUserDetails("user1@test.com")
    void deleteByNonAdmin() throws Exception {
        Member owner = saveMember("adm-ctrl-delete-nonadmin-owner@test.com");
        PartyDto created = createParty(owner, "컨트롤러 강제삭제 권한 테스트 파티");

        mvc.perform(delete("/api/v1/adm/parties/" + created.id()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(partyRepository.findById(created.id())).isPresent();
    }

    private Member saveMember(String email) {
        Member member = memberRepository.save(new Member(email, "pw", "닉네임", null));
        memberProfileRepository.save(new MemberProfile(member, null, null, PositionType.BACK, List.of()));
        return member;
    }

    private PartyDto createParty(Member owner, String title) {
        return partyService.create(
                owner, "파티명", title, null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );
    }
}
