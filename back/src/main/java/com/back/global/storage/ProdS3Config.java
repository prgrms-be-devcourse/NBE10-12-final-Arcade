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
        CustomConfigProperties.Storage.S3 properties = customConfigProperties.getStorage().getS3();

        //환경 변수가 없으면 박아둔 http://localhost:9000를 이용해서 nullpointexception 예방
        // 서버 설정시 꼭 환경변수 전달해야함
        if (properties.getPublicUrlPrefix() == null || properties.getPublicUrlPrefix().isBlank()) {
            throw new IllegalStateException(
                    "prod 에서 S3 저장소를 쓰려면 CUSTOM__STORAGE__S3__PUBLIC_URL_PREFIX 를 설정해야 합니다. "
                            + "(예: https://<bucket>.s3.<region>.amazonaws.com 또는 CloudFront 도메인)");
        }

        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
