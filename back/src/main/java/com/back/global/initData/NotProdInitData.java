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
import org.springframework.context.annotation.Profile;
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
    ApplicationRunner notProdInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        Member memberSystem = memberService.join("system", "1234", "시스템");
        memberSystem.modifyApiKey(memberSystem.getEmail());

        Member memberAdmin = memberService.join("admin", "1234", "관리자");
        memberAdmin.modifyApiKey(memberAdmin.getEmail());

        Member memberUser1 = memberService.join("user1", "1234", "유저1");
        memberUser1.modifyApiKey(memberUser1.getEmail());

        Member memberUser2 = memberService.join("user2", "1234", "유저2");
        memberUser2.modifyApiKey(memberUser2.getEmail());

        Member memberUser3 = memberService.join("user3", "1234", "유저3");
        memberUser3.modifyApiKey(memberUser3.getEmail());

        customConfigProperties.getNotProdMembers().forEach(notProdMember -> {
            Member socialMember = memberService.join(notProdMember.username(), null, notProdMember.nickname(), notProdMember.profileImgUrl());
            socialMember.modifyApiKey(notProdMember.apiKey());
        });
    }

    @Transactional
    public void work2() {

        Member memberUser1 = memberService.findByUsername("user1").get();
        Member memberUser2 = memberService.findByUsername("user2").get();
        Member memberUser3 = memberService.findByUsername("user3").get();

    }
}
