package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.s3.S3Client;

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

    /** S3Client 는 프로파일별로 다르다. DevS3Config(MinIO) / ProdS3Config(실제 S3). */
    @Bean
    @ConditionalOnProperty(name = "custom.storage.type", havingValue = "s3")
    FileStorage s3FileStorage(S3Client s3Client) {
        return new S3FileStorage(s3Client, customConfigProperties.getStorage().getS3());
    }
}
