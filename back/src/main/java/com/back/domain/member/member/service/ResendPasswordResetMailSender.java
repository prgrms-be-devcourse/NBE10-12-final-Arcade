package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.app.CustomConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/** Resend Email API를 통한 비밀번호 재설정 안내 메일 발송 구현이다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "custom.mail", name = "enabled", havingValue = "true")
public class ResendPasswordResetMailSender implements PasswordResetMailSender {
    private static final String SUBJECT = "[Arcade] 비밀번호 재설정 안내";

    private final RestClient restClient;
    private final CustomConfigProperties.Mail mail;
    private final String frontendBaseUrl;

    public ResendPasswordResetMailSender(
            RestClient.Builder restClientBuilder,
            CustomConfigProperties customConfigProperties,
            @Value("${custom.frontend.base-url:/}") String frontendBaseUrl
    ) {
        this.mail = customConfigProperties.getMail();
        if (mail.getResend().getApiKey().isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY must be configured when custom.mail.enabled=true");
        }
        this.restClient = restClientBuilder.baseUrl(mail.getResend().getApiBaseUrl()).build();
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void send(Member member, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/password/reset")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        Map<String, Object> body = Map.of(
                "from", mail.getFrom(),
                "to", List.of(member.getEmail()),
                "subject", SUBJECT,
                "html", html(resetUrl),
                "text", text(resetUrl)
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mail.getResend().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
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
