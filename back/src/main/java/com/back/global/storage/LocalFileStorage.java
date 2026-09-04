package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** MinIO·S3 없이 로컬 디스크에 저장한다. StorageConfig 가 정적 경로로 열어준다. */
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final CustomConfigProperties.Storage.Local properties;

    @Override
    public String upload(MultipartFile file, String directory) {
        String key = FileStorage.newKey(directory, file.getOriginalFilename());
        Path target = Path.of(properties.getPath()).toAbsolutePath().resolve(key).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("500-1", "파일 저장에 실패했습니다.");
        }

        return properties.getUrlPrefix() + "/" + key;
    }
}
