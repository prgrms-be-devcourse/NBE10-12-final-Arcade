package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.app.CustomConfigProperties;
import com.resend.core.exception.ResendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** Resend Java SDK를 통한 비밀번호 재설정 안내 메일 발송 구현이다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "custom.mail", name = "enabled", havingValue = "true")
public class ResendPasswordResetMailSender implements PasswordResetMailSender {
    private static final String SUBJECT = "[Arcade] 비밀번호 재설정 안내";

    private final CustomConfigProperties.Mail mail;
    private final String frontendBaseUrl;
    private final ResendEmailGateway resendEmailGateway;

    public ResendPasswordResetMailSender(
            CustomConfigProperties customConfigProperties,
            @Value("${custom.frontend.base-url:/}") String frontendBaseUrl,
            ResendEmailGateway resendEmailGateway
    ) {
        this.mail = customConfigProperties.getMail();
        this.frontendBaseUrl = frontendBaseUrl;
        this.resendEmailGateway = resendEmailGateway;
    }

    @Override
    public void send(Member member, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/password/reset")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        try {
            resendEmailGateway.send(mail.getFrom(), member.getEmail(), SUBJECT, html(resetUrl), text(resetUrl));
        } catch (ResendException | IllegalStateException e) {
            // 외부 응답에는 실패 여부를 드러내지 않으며 수신 이메일·토큰·링크도 기록하지 않는다.
            log.error("security_event=password_reset_mail_failed memberId={}", member.getId());
        }
    }

    private String html(String resetUrl) {
        String escapedUrl = HtmlUtils.htmlEscape(resetUrl);
        return """
                <p>Arcade 비밀번호 재설정 안내입니다.</p>
                <p><a href="%s">비밀번호 재설정하기</a></p>
                <p>이 링크는 30분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
                """.formatted(escapedUrl);
    }

    private String text(String resetUrl) {
        return """
                Arcade 비밀번호 재설정 안내입니다.

                아래 링크에서 비밀번호를 재설정해주세요.
                %s

                이 링크는 30분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.
                """.formatted(resetUrl);
    }
}
