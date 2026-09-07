package com.back.global.app;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import org.springframework.util.unit.DataSize;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "custom")
@Getter
@Setter
public class CustomConfigProperties {
    private List<NotProdMember> notProdMembers;
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private Storage storage = new Storage();

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
}
