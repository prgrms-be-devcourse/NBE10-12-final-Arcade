package com.back.global.initData;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.app.CustomConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestContainerConfig.class)
public class BaseInitDataTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CustomConfigProperties customConfigProperties;

    @Test
    @DisplayName("시드 관리자 계정에는 약관 동의가 기록돼 있다")
    void adminAccountHasAgreement() {
        String email = customConfigProperties.getAdminAccount().getEmail();
        Member admin = memberRepository.findByEmail(email).orElseThrow();

        assertThat(admin.isAdmin()).isTrue();
        // 동의 기록이 없으면 MemberAgreementGuardFilter 를 켜는 순간 관리자 콘솔부터 막힌다.
        // 필터에 역할 예외를 두지 않는 대신 여기서 채운다.
        assertThat(admin.hasAgreedToRequiredTerms()).isTrue();
    }
}
