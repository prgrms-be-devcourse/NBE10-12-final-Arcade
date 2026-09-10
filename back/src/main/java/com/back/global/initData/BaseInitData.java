package com.back.global.initData;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 프로필과 무관하게(로컬/dev/test/prod 전부) 항상 실행된다.
// custom.admin-account.email/password 로 admin 계정 존재를 보장한다 (멱등).
@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;
    private final CustomConfigProperties customConfigProperties;

    @Bean
    @Order(3)
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.ensureAdminAccount();
        };
    }

    @Transactional
    public void ensureAdminAccount() {
        String email = customConfigProperties.getAdminAccount().getEmail();
        String password = customConfigProperties.getAdminAccount().getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        if (memberService.findByEmail(email).isEmpty()) {
            memberService.join(email, password, customConfigProperties.getAdminAccount().getNickname());
        }

        memberService.findByEmail(email).ifPresent(admin -> {
            if (!admin.isAdmin()) {
                admin.grantAdmin();
            }

            // 운영 계정도 약관 동의 기록이 있어야 한다. 없으면 동의 강제 필터가 켜지는 순간
            // 관리자 콘솔부터 막히는데, 온보딩은 회원용 화면이라 통과할 방법이 없다.
            // 필터에 역할 예외를 두는 대신 여기서 채운다 - 규칙을 하나로 유지한다.
            admin.agreeToRequiredTerms(LocalDateTime.now());
        });
    }
}
