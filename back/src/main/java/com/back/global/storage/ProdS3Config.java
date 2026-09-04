package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** prod 는 실제 S3. 자격증명은 인스턴스 역할·환경변수 등 기본 체인에서 찾는다. */
@Profile("prod")
@Configuration
@ConditionalOnProperty(name = "custom.storage.type", havingValue = "s3")
public class ProdS3Config {

    @Bean
    S3Client s3Client(CustomConfigProperties customConfigProperties) {
        return S3Client.builder()
                .region(Region.of(customConfigProperties.getStorage().getS3().getRegion()))
                .build();
    }
}
