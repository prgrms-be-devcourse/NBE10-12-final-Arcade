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

    public record NotProdMember(
            String username,
            String apiKey,
            String nickname,
            String profileImgUrl
    ) {
    }
}
