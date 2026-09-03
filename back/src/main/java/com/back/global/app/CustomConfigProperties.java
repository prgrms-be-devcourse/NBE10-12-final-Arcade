package com.back.global.app;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "custom")
@Getter
@Setter
public class CustomConfigProperties {
    private List<NotProdMember> notProdMembers;
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();

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
}
