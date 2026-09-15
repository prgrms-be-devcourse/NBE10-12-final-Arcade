package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;

/** 비밀번호 재설정 관련 메일 제공자와 도메인 로직 사이의 경계다. */
public interface PasswordResetMailSender {
    void sendResetLink(Member member, String token);

    void sendResetCompleted(Member member);
}
