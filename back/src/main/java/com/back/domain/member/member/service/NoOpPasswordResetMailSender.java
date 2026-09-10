package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import org.springframework.stereotype.Component;

/** 메일 공급자 연동 전 기본 구현. 이메일·토큰·링크를 로그에 남기지 않는다. */
@Component
public class NoOpPasswordResetMailSender implements PasswordResetMailSender {
    @Override
    public void send(Member member, String token) {
        // 실제 발송은 후속 메일 발송 연동 작업에서 구현한다.
    }
}
