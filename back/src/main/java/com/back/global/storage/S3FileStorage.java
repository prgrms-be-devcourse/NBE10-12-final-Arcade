package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

/** S3 및 S3 호환 저장소(MinIO). */
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final CustomConfigProperties.Storage.S3 properties;

    @Override
    public String upload(MultipartFile file, String directory) {
        String key = FileStorage.newKey(directory, file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            throw new ServiceException("500-1", "파일 저장에 실패했습니다.");
        }

        return publicUrlPrefix() + "/" + key;
    }

    private String publicUrlPrefix() {
        String configured = properties.getPublicUrlPrefix();

        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", "");
        }

        String endpoint = properties.getEndpoint();

        // endpoint 는 MinIO 를 바라보는 dev·test 이용 (DevS3Config).
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "S3 공개 URL 을 만들 수 없습니다. custom.storage.s3.public-url-prefix "
                            + "또는 custom.storage.s3.endpoint 중 하나를 설정해야 합니다.");
        }

        return endpoint.replaceAll("/+$", "") + "/" + properties.getBucket();
    }
}
