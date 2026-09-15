package com.back.domain.member.member.service;

import com.back.global.app.CustomConfigProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "custom.mail", name = "enabled", havingValue = "true")
public class ResendSdkEmailGateway implements ResendEmailGateway {
    private final CustomConfigProperties customConfigProperties;

    @Override
    public void send(String from, String to, String subject, String html, String text) throws ResendException {
        String apiKey = customConfigProperties.getMail().getResend().getApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY must be configured when custom.mail.enabled=true");
        }

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(html)
                .text(text)
                .build();
        new Resend(apiKey).emails().send(options);
    }
}
