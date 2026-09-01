package com.back.global.github.service;

import com.back.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** GitHub webhook 원본 body의 HMAC-SHA256 서명을 검증한다. */
@Component
public class GithubWebhookVerifier {
    @Value("${custom.github.webhook.secret:}")
    private String webhookSecret;

    public void verify(String signature, byte[] body) {
        if (webhookSecret == null || webhookSecret.isBlank()) throw new IllegalStateException("GitHub webhook secret is not configured.");
        if (signature == null || !signature.startsWith("sha256=")) throw new ServiceException("401-1", "GitHub 웹훅 서명이 없습니다.");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                throw new ServiceException("401-1", "GitHub 웹훅 서명이 올바르지 않습니다.");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("GitHub 웹훅 서명을 검증할 수 없습니다.", e);
        }
    }
}
