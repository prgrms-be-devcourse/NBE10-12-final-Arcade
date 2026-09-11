package com.back.domain.member.member.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdminMemberControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberProfileRepository memberProfileRepository;

    private long targetId;

    @BeforeEach
    void setUp() {
        Member target = memberRepository.save(new Member("adm-ctrl-member-target@test.com", "pw", "회원A", null));
        memberProfileRepository.save(new MemberProfile(target, "닉네임A", null, PositionType.FRONT, List.of("React")));
        targetId = target.getId();
    }

    @Test
    @DisplayName("관리자 회원 목록: 관리자가 조회하면 200이다")
    @WithUserDetails("admin")
    void listByAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("관리자 회원 목록: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void listByNonAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/members"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("관리자 회원 목록: 로그인하지 않았으면 401-1이다")
    void listWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/adm/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("관리자 회원 상세: position/techStacks 포함")
    @WithUserDetails("admin")
    void detailByAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/members/" + targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.nickname").value("닉네임A"))
                .andExpect(jsonPath("$.data.position").value("FRONT"))
                .andExpect(jsonPath("$.data.techStacks[0]").value("React"));
    }

    @Test
    @DisplayName("회원 정지: 관리자가 요청하면 200이고 실제로 정지된다")
    @WithUserDetails("admin")
    void suspendByAdmin() throws Exception {
        ResultActions resultActions = mvc.perform(
                patch("/api/v1/adm/members/%d/status".formatted(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"active": false, "reason": "부적절한 활동"}
                            """)
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        Member updated = memberRepository.findById(targetId).orElseThrow();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getSuspendReason()).isEqualTo("부적절한 활동");
    }

    @Test
    @DisplayName("회원 정지 해제: 관리자가 요청하면 200이고 실제로 해제된다")
    @WithUserDetails("admin")
    void activateByAdmin() throws Exception {
        Member target = memberRepository.findById(targetId).orElseThrow();
        target.suspend("이전 정지 사유");

        ResultActions resultActions = mvc.perform(
                patch("/api/v1/adm/members/%d/status".formatted(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"active": true}
                            """)
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        Member updated = memberRepository.findById(targetId).orElseThrow();
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getSuspendReason()).isNull();
    }

    @Test
    @DisplayName("회원 정지: 관리자가 아니면 403-1이고 정지되지 않는다")
    @WithUserDetails("user1@test.com")
    void suspendByNonAdmin() throws Exception {
        mvc.perform(
                        patch("/api/v1/adm/members/%d/status".formatted(targetId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"active": false, "reason": "부적절한 활동"}
                            """)
                ).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(memberRepository.findById(targetId).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("회원 정지: active 없이 요청하면 400이다")
    @WithUserDetails("admin")
    void updateStatusWithoutActiveReturns400() throws Exception {
        mvc.perform(
                patch("/api/v1/adm/members/%d/status".formatted(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 회원 이력: 관리자가 조회하면 파티 이력과 성취 이력이 함께 온다")
    @WithUserDetails("admin")
    void historyByAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/members/" + targetId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.partyHistory.ownedParties").isArray())
                .andExpect(jsonPath("$.data.partyHistory.appliedParties").isArray())
                .andExpect(jsonPath("$.data.achievements").isArray());
    }

    @Test
    @DisplayName("관리자 회원 이력: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void historyByNonAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/members/" + targetId + "/history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }
}
