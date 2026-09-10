package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.app.CustomConfigProperties;
import com.resend.core.exception.ResendException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResendPasswordResetMailSenderTest {
    @Test
    void sendsPasswordResetEmailThroughResendApi() throws Exception {
        ResendEmailGateway gateway = mock(ResendEmailGateway.class);
        ResendPasswordResetMailSender sender = sender(gateway);
        Member member = new Member("recipient@example.com", null, "회원", null);

        sender.send(member, "url-safe_token");

        verify(gateway).send(
                eq("Arcade <onboarding@resend.dev>"),
                eq("recipient@example.com"),
                eq("[Arcade] 비밀번호 재설정 안내"),
                contains("http://localhost:3000/password/reset?token=url-safe_token"),
                contains("http://localhost:3000/password/reset?token=url-safe_token")
        );
    }

    @Test
    void hidesResendFailureFromPasswordResetRequestFlow() throws Exception {
        ResendEmailGateway gateway = mock(ResendEmailGateway.class);
        ResendPasswordResetMailSender sender = sender(gateway);
        Member member = new Member("recipient@example.com", null, "회원", null);
        doThrow(mock(ResendException.class)).when(gateway)
                .send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());

        assertThatCode(() -> sender.send(member, "url-safe_token")).doesNotThrowAnyException();
    }

    private ResendPasswordResetMailSender sender(ResendEmailGateway gateway) {
        CustomConfigProperties properties = new CustomConfigProperties();
        properties.getMail().setEnabled(true);
        properties.getMail().setFrom("Arcade <onboarding@resend.dev>");
        properties.getMail().getResend().setApiKey("resend-test-key");
        return new ResendPasswordResetMailSender(properties, "http://localhost:3000", gateway);
    }
}
