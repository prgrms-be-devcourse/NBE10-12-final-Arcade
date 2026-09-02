package com.back.global.initData;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Profile("!prod")
@Configuration
@RequiredArgsConstructor
public class NotProdInitData {
    @Autowired
    @Lazy
    private NotProdInitData self;
    private final MemberService memberService;
    private final CustomConfigProperties customConfigProperties;

    @Bean
    @Order(1)
    ApplicationRunner notProdInitDataApplicationRunner() {
        return args -> {
            self.work1();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        createMemberWithApiKey("system", "1234", "시스템", "system");

        createMemberWithApiKey("admin", "1234", "관리자", "admin");

        createMemberWithApiKey("user1@test.com", "1234", "유저1", "user1");

        createMemberWithApiKey("user2@test.com", "1234", "유저2", "user2");

        createMemberWithApiKey("user3@test.com", "1234", "유저3", "user3");

        customConfigProperties.getNotProdMembers().forEach(notProdMember -> {
            createMemberWithApiKey(
                    notProdMember.username(),
                    null,
                    notProdMember.nickname(),
                    notProdMember.apiKey()
            );

        });
        memberService.findByEmail("system").ifPresent(Member::grantAdmin);
        memberService.findByEmail("admin").ifPresent(Member::grantAdmin);
    }

    private void createMemberWithApiKey(String email, String password, String name, String apiKey) {
        MemberDto member = memberService.join(email, password, name);
        memberService.modifyApiKey(member.id(), apiKey);
    }

}
