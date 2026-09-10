package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;

/** 실제 메일 제공자 연동 전까지는 테스트 대역으로 교체할 수 있는 경계다. */
public interface PasswordResetMailSender {
    void send(Member member, String token);
}
