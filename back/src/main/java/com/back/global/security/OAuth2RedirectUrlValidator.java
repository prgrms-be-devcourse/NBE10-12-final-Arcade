package com.back.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

@Component
public class OAuth2RedirectUrlValidator {
    private final URI frontendBaseUri;

    public OAuth2RedirectUrlValidator(@Value("${custom.frontend.base-url:/}") String frontendBaseUrl) {
        this.frontendBaseUri = URI.create(frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/");
    }

    /**
     * OAuth2 완료 후 이동할 URL은 프론트엔드 base URL과 동일한 origin 또는 내부 상대 경로만 허용한다.
     */
    public String getSafeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return frontendBaseUri.toString();
        }

        try {
            URI uri = URI.create(redirectUrl);
            if (isInternalRelativePath(redirectUrl, uri)) {
                return frontendBaseUri.resolve(redirectUrl.substring(1)).toString();
            }

            if (isFrontendOrigin(uri)) {
                return uri.toString();
            }

            return frontendBaseUri.toString();
        } catch (IllegalArgumentException e) {
            return frontendBaseUri.toString();
        }
    }

    private boolean isInternalRelativePath(String redirectUrl, URI uri) {
        return !uri.isAbsolute()
                && uri.getRawAuthority() == null
                && redirectUrl.startsWith("/")
                && !redirectUrl.startsWith("//")
                // 첫 '/'를 제거한 값이 URL 스킴으로 해석되면 resolve가 외부 URL로 전환할 수 있다.
                && !URI.create(redirectUrl.substring(1)).isAbsolute()
                && !redirectUrl.contains("\\");
    }

    private boolean isFrontendOrigin(URI uri) {
        return uri.isAbsolute()
                && uri.getRawUserInfo() == null
                && Objects.equals(uri.getScheme(), frontendBaseUri.getScheme())
                && Objects.equals(uri.getHost(), frontendBaseUri.getHost())
                && getEffectivePort(uri) == getEffectivePort(frontendBaseUri);
    }

    private int getEffectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }

        return switch (uri.getScheme()) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }
}
