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
        });
    }
}
