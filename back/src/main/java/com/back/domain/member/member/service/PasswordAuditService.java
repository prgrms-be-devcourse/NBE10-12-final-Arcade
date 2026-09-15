package com.back.domain.member.member.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 비밀번호·토큰·이메일 없이 성공한 보안 이벤트만 감사 로그로 기록한다. */
@Component
public class PasswordAuditService {
    private static final Logger log = LoggerFactory.getLogger(PasswordAuditService.class);

    public void passwordReset(long memberId) {
        log.info("security_event=password_reset_succeeded memberId={}", memberId);
    }

    public void passwordChanged(long memberId) {
        log.info("security_event=password_changed memberId={}", memberId);
    }
}
