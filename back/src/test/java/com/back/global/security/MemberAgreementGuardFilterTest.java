package com.back.global.security;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 약관 미동의 차단 필터. 이 클래스만 설정으로 필터를 켠다.
 *
 * 다른 테스트는 필터가 없는 상태로 돌아야 한다 - 기존 계정이 전부 미동의라
 * 전역으로 켜면 무관한 테스트가 403 으로 무너진다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
@TestPropertySource(properties = "custom.agreement.guard.enabled=true")
public class MemberAgreementGuardFilterTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("미동의 계정은 보호 API 에서 403-2 로 막힌다")
    @WithUserDetails("user1@test.com")
    void blocksMemberWithoutAgreement() throws Exception {
        // 403-1(권한 없음)과 다른 코드여야 화면이 온보딩으로 보낼 수 있다
        mvc.perform(get("/api/v1/members/me/bookmarks"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-2"));
    }

    @Test
    @DisplayName("미동의 계정도 동의 API·내 정보·로그아웃은 부를 수 있다")
    @WithUserDetails("user1@test.com")
    void allowsEscapeHatches() throws Exception {
        // 동의하러 가는 길을 막으면 아무것도 할 수 없는 계정이 된다
        mvc.perform(post("/api/v1/members/me/agreements"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/members/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("동의한 계정은 그대로 통과한다")
    @WithUserDetails("user1@test.com")
    void passesAgreedMember() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        actor.agreeToRequiredTerms(LocalDateTime.now());
        memberRepository.saveAndFlush(actor);

        mvc.perform(get("/api/v1/members/me/bookmarks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자로 올리면 동의가 함께 기록돼 막히지 않는다")
    @WithUserDetails("user2@test.com")
    void adminGetsAgreementOnGrant() throws Exception {
        Member actor = memberRepository.findByEmail("user2@test.com").orElseThrow();
        assertThat(actor.hasAgreedToRequiredTerms()).isFalse();

        // 필터에 역할 예외를 두지 않는다 - 두면 "관리자는 동의 없이 서비스 이용" 이라는 구멍이 생긴다.
        // 대신 관리자가 되는 시점에 동의를 기록해 규칙을 하나로 유지한다.
        actor.grantAdmin();
        memberRepository.saveAndFlush(actor);

        assertThat(actor.hasAgreedToRequiredTerms()).isTrue();

        mvc.perform(get("/api/v1/members/me/bookmarks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비인증 공개 조회는 필터가 건드리지 않는다")
    void ignoresPublicEndpoints() throws Exception {
        // 로그인하지 않은 요청은 이 필터가 판단할 문제가 아니다 - 인가 규칙에 맡긴다
        mvc.perform(get("/api/v1/parties"))
                .andExpect(status().isOk());
    }
}
