package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.app.CustomConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendPasswordResetMailSenderTest {
    @Test
    void sendsPasswordResetEmailThroughResendApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendPasswordResetMailSender sender = sender(builder);
        Member member = new Member("recipient@example.com", null, "회원", null);

        server.expect(requestTo("https://resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer resend-test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value("Arcade <onboarding@resend.dev>"))
                .andExpect(jsonPath("$.to[0]").value("recipient@example.com"))
                .andExpect(jsonPath("$.subject").value("[Arcade] 비밀번호 재설정 안내"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString(
                        "http://localhost:3000/password/reset?token=url-safe_token")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        sender.send(member, "url-safe_token");

        server.verify();
    }

    @Test
    void hidesResendFailureFromPasswordResetRequestFlow() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendPasswordResetMailSender sender = sender(builder);
        Member member = new Member("recipient@example.com", null, "회원", null);

        server.expect(requestTo("https://resend.test/emails"))
                .andRespond(withServerError());

        assertThatCode(() -> sender.send(member, "url-safe_token")).doesNotThrowAnyException();

        server.verify();
    }

    private ResendPasswordResetMailSender sender(RestClient.Builder builder) {
        CustomConfigProperties properties = new CustomConfigProperties();
        properties.getMail().setEnabled(true);
        properties.getMail().setFrom("Arcade <onboarding@resend.dev>");
        properties.getMail().getResend().setApiKey("resend-test-key");
        properties.getMail().getResend().setApiBaseUrl("https://resend.test");
        return new ResendPasswordResetMailSender(builder, properties, "http://localhost:3000");
    }
}
