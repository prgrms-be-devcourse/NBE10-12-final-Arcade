package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 메일 공급자 연동 전 기본 구현. 이메일·토큰·링크를 로그에 남기지 않는다. */
@Component
@ConditionalOnProperty(prefix = "custom.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPasswordResetMailSender implements PasswordResetMailSender {
    @Override
    public void sendResetLink(Member member, String token) {
        // 메일 발송을 끈 환경에서는 의도적으로 아무 작업도 하지 않는다.
    }

    @Override
    public void sendResetCompleted(Member member) {
        // 메일 발송을 끈 환경에서는 의도적으로 아무 작업도 하지 않는다.
    }
}
