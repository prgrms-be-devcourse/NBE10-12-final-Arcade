package com.back.global.app;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import org.springframework.util.unit.DataSize;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "custom")
@Validated
@Getter
@Setter
public class CustomConfigProperties {
    private List<NotProdMember> notProdMembers;
    @Valid
    private Cookie cookie = new Cookie();
    @Valid
    private Cors cors = new Cors();
    @Valid
    private Storage storage = new Storage();
    @Valid
    private AdminAccount adminAccount = new AdminAccount();
    @Valid
    private Password password = new Password();
    @Valid
    private PasswordReset passwordReset = new PasswordReset();
    @Valid
    private RateLimit rateLimit = new RateLimit();
    @Valid
    private Session session = new Session();
    @Valid
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;
        @NotBlank
        private String from = "Arcade <onboarding@resend.dev>";
        @Valid
        private Resend resend = new Resend();

        @Getter
        @Setter
        public static class Resend {
            private String apiKey = "";
            @NotBlank
            private String apiBaseUrl = "https://api.resend.com";
        }
    }

    @Getter
    @Setter
    public static class Password {
        @Min(1)
        private int minLength = 8;
        @Min(1)
        private int maxLength = 64;
        private boolean requireLetter = true;
        private boolean requireNumber = true;
        private boolean requireSpecialCharacter = true;
        private boolean allowWhitespace = false;

        @AssertTrue(message = "custom.password.min-length must not exceed max-length")
        public boolean isLengthRangeValid() {
            return minLength <= maxLength;
        }
    }

    @Getter
    @Setter
    public static class PasswordReset {
        @Min(1)
        private long expirationSeconds = 1800;
    }

    @Getter
    @Setter
    public static class RateLimit {
        @Valid
        private PasswordResetRequest passwordResetRequest = new PasswordResetRequest();

        @Getter
        @Setter
        public static class PasswordResetRequest {
            @Min(1)
            private int emailMaxRequests = 3;
            @Min(1)
            private int ipMaxRequests = 10;
            @Min(1)
            private long windowSeconds = 3600;
        }
    }

    @Getter
    @Setter
    public static class Session {
        /**
         * 현재 선택한 최소안: 비밀번호 변경 시 refresh token만 폐기하고 기존 access token은 만료까지 허용한다.
         * 즉시 무효화가 필요해지면 auth-version 전략을 구현한 뒤 이 값을 변경한다.
         */
        @NotBlank
        private String accessTokenInvalidation = "refresh-token-only";
    }

    @Getter
    @Setter
    public static class Cookie {
        private boolean secure;
        private String sameSite = "Strict";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of();
    }

    public record NotProdMember(
            String username,
            String apiKey,
            String nickname,
            String profileImgUrl
    ) {
    }

    @Getter
    @Setter
    public static class Storage {
        private Type type = Type.LOCAL;
        private DataSize maxFileSize = DataSize.ofMegabytes(5);
        private Local local = new Local();
        private S3 s3 = new S3();

        public enum Type {
            LOCAL, S3
        }

        @Getter
        @Setter
        public static class Local {
            private String path = ".uploads";
            private String urlPrefix = "/uploads";
        }

        @Getter
        @Setter
        public static class S3 {
            private String endpoint;
            private String region = "us-east-1";
            private String bucket;
            private String accessKey;
            private String secretKey;
            /** 비우면 endpoint/bucket 으로 만든다. */
            private String publicUrlPrefix;
            /** MinIO 는 버킷을 서브도메인이 아니라 경로로 받는다. */
            private boolean pathStyleAccess = true;
        }
    }

    @Getter
    @Setter
    public static class AdminAccount {
        private String email = "admin@test.com";
        private String password = "1234";
        private String nickname = "관리자";
    }
}
