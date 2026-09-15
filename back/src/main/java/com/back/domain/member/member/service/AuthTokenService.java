package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import com.back.standard.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthTokenService {
    public record AccessTokenPayload(long memberId, Role role) {
    }

    @Value("${custom.accessToken.expirationSeconds}")
    private int expireSeconds;

    @Value("${custom.jwt.secretKey}")
    private String secret;

    String genAccessToken(Member member) {
        long id = member.getId();
        String role = member.getRole().name();

        return Util.jwt.toString(
                secret,
                expireSeconds,
                Map.of("id", id, "role", role)
        );
    }

    AccessTokenPayload payload(String accessToken) {
        Map<String, Object> parsedPayload = Util.jwt.payload(secret, accessToken);

        if (parsedPayload == null) return null;

        Object id = parsedPayload.get("id");
        Object role = parsedPayload.get("role");

        if (!(id instanceof Number number) || !(role instanceof String roleName)) {
            return null;
        }

        try {
            return new AccessTokenPayload(number.longValue(), Role.valueOf(roleName));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
