package com.back.global.storage;

import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

        // Files.copy 는 넘겨받은 스트림을 닫지 않는다(Javadoc 명시).
        // 서블릿 멀티파트는 디스크 임시 파일을 열어두므로, 안 닫으면 FD 가 물린 채 남는다.
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("500-1", "파일 저장에 실패했습니다.");
        }

        return properties.getUrlPrefix() + "/" + key;
    }

    @Override
    public String uploadKey(MultipartFile file, String directory) {
        String key = FileStorage.newKey(directory, file.getOriginalFilename());
        Path target = Path.of(properties.getPath()).toAbsolutePath().resolve(key).normalize();

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("500-1", "파일 저장에 실패했습니다.");
        }

        return key;
    }
}
