package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.entity.EvidenceStatus;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 관리자 증빙 검수. 업로드부터 태워서 실제로 저장된 파일을 받아오는 것까지 본다. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdminGoalControllerTest {

    private static final String OWNER = "user1@test.com";
    private static final byte[] PDF = "%PDF-1.4 수상확인서 더미".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GoalService goalService;

    private long goalId;

    @BeforeEach
    void setUp() {
        Member owner = memberRepository.findByEmail(OWNER).orElseThrow();

        goalId = goalRepository.save(new PersonalContest(
                owner, GoalStatus.ACHIEVED, "전국 대학생 해커톤", false,
                "장려상", LocalDate.of(2023, 11, 15), "https://example.com/contest"
        )).getId();
    }

    /**
     * 검수할 대상 만들기 - 소유자가 올린 상태로 맞춘다.
     *
     * 업로드 API 를 태우지 않고 서비스로 부른다. 테스트 메서드는 @WithUserDetails("admin") 으로 도는데,
     * 업로드는 소유자만 할 수 있어서 같은 요청 흐름으로는 403 이 된다.
     */
    private void uploadEvidence() {
        goalService.uploadEvidence(
                memberRepository.findByEmail(OWNER).orElseThrow(),
                goalId,
                new MockMultipartFile("file", "수상확인서.pdf", "application/pdf", PDF)
        );
    }

    @Test
    @DisplayName("검수 목록: 올라온 증빙이 PENDING 으로 잡힌다")
    @WithUserDetails("admin")
    void listPending() throws Exception {
        uploadEvidence();

        mvc.perform(get("/api/v1/adm/goals/evidences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content[?(@.goalId == %d)].fileName".formatted(goalId))
                        .value("수상확인서.pdf"))
                .andExpect(jsonPath("$.data.content[?(@.goalId == %d)].status".formatted(goalId))
                        .value("PENDING"));
    }

    @Test
    @DisplayName("다운로드: 저장된 파일이 첨부파일로 내려온다")
    @WithUserDetails("admin")
    void download() throws Exception {
        uploadEvidence();

        mvc.perform(get("/api/v1/adm/goals/%d/evidence".formatted(goalId)))
                .andExpect(status().isOk())
                // 저장된 mimeType(application/pdf)이 아니라 첨부파일로 고정해서 내려준다
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    @DisplayName("반려: 사유가 없으면 400-1 이고 상태도 안 바뀐다")
    @WithUserDetails("admin")
    void rejectWithoutNote() throws Exception {
        uploadEvidence();

        mvc.perform(patch("/api/v1/adm/goals/%d/evidence".formatted(goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        assertThat(contest().getEvidenceStatus()).isEqualTo(EvidenceStatus.PENDING);
    }

    @Test
    @DisplayName("반려: 사유와 함께 반려하면 상태와 사유가 남는다")
    @WithUserDetails("admin")
    void reject() throws Exception {
        uploadEvidence();

        mvc.perform(patch("/api/v1/adm/goals/%d/evidence".formatted(goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "note": "확인서가 흐릿해서 대회명이 안 보여요"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        assertThat(contest().getEvidenceStatus()).isEqualTo(EvidenceStatus.REJECTED);
        assertThat(contest().getEvidenceReviewNote()).isEqualTo("확인서가 흐릿해서 대회명이 안 보여요");
    }

    @Test
    @DisplayName("증빙이 없는 성취는 404-1 이다")
    @WithUserDetails("admin")
    void reviewWithoutEvidence() throws Exception {
        mvc.perform(patch("/api/v1/adm/goals/%d/evidence".formatted(goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPROVED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("관리자가 아니면 검수 목록이 403-1 이다")
    @WithUserDetails(OWNER)
    void listByNonAdmin() throws Exception {
        mvc.perform(get("/api/v1/adm/goals/evidences"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    private PersonalContest contest() {
        return (PersonalContest) goalRepository.findById(goalId).orElseThrow();
    }
}
