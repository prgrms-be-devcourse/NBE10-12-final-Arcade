package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final CustomConfigProperties customConfigProperties;

    @Bean
    @ConditionalOnProperty(name = "custom.storage.type", havingValue = "local", matchIfMissing = true)
    FileStorage localFileStorage() {
        return new LocalFileStorage(customConfigProperties.getStorage().getLocal());
    }

    /** local 로 저장한 파일을 그대로 내려받을 수 있게 정적 경로를 연다. */
    @Bean
    @ConditionalOnProperty(name = "custom.storage.type", havingValue = "local", matchIfMissing = true)
    WebMvcConfigurer localStorageResourceHandler() {
        CustomConfigProperties.Storage.Local properties = customConfigProperties.getStorage().getLocal();
        String location = Path.of(properties.getPath()).toAbsolutePath().toUri().toString();

        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler(properties.getUrlPrefix() + "/**")
                        .addResourceLocations(location);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "custom.storage.type", havingValue = "s3")
    FileStorage s3FileStorage() {
        CustomConfigProperties.Storage.S3 properties = customConfigProperties.getStorage().getS3();

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                // MinIO 는 버킷을 서브도메인이 아니라 경로로 받는다.
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build())
                .build();

        return new S3FileStorage(s3Client, properties);
    }
}
