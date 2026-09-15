package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 업로드 공통 검증 — jpg/png, 빈 파일, 용량 상한.
 * 프로필 사진·대회 대표 이미지 등 여러 도메인이 같은 규칙을 쓰므로 한 곳에 모은다.
 */
@Component
@RequiredArgsConstructor
public class ImageUploadValidator {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png");

    private final CustomConfigProperties customConfigProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("400-1", "이미지 파일이 비어 있습니다.");
        }

        // Content-Type 헤더가 없는 파트면 getContentType() 이 null 이다.
        // List.of() 로 만든 목록은 contains(null) 에서 NPE 를 내므로 먼저 거른다.
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ServiceException("400-1", "jpg, png 이미지만 올릴 수 있습니다.");
        }

        long maxBytes = customConfigProperties.getStorage().getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new ServiceException("400-1",
                    "이미지는 %dMB 까지 올릴 수 있습니다.".formatted(maxBytes / 1024 / 1024));
        }
    }
}
