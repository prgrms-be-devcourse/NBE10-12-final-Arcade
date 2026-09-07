package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** dev·test 에서는 로컬 MinIO 를 S3 대신 바라본다. docker-minio.yaml 참고. */
@Profile({"dev", "test"})
@Configuration
@ConditionalOnProperty(name = "custom.storage.type", havingValue = "s3")
public class DevS3Config {

    @Bean
    S3Client s3Client(CustomConfigProperties customConfigProperties) {
        CustomConfigProperties.Storage.S3 properties = customConfigProperties.getStorage().getS3();

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build())
                .build();
    }
}
