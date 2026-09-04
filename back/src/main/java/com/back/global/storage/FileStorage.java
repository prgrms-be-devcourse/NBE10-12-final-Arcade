package com.back.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public interface FileStorage {

    /** 저장을 허용하는 확장자. 이 밖은 확장자 없이 저장한다. */
    Set<String> SAFE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");

    /** 파일을 저장하고 외부에서 접근할 수 있는 URL 을 돌려준다. */
    String upload(MultipartFile file, String directory);

    /** 확장자만 남긴 랜덤 키. 원본 이름은 그대로 쓰지 않는다. */
    static String newKey(String directory, String originalFilename) {
        return directory + "/" + UUID.randomUUID() + safeExtension(originalFilename);
    }

    /**
     * 파일명은 사용자가 정하는 값이라 그대로 믿지 않는다.
     * 경로 조각을 떼고 허용 목록에 있는 확장자만 남긴다.
     * 확장자를 거르지 않으면 .html 로 올린 파일이 우리 도메인에서 text/html 로 실행된다.
     */
    private static String safeExtension(String originalFilename) {
        if (originalFilename == null) return "";

        String name = originalFilename;
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String fileName = separator > -1 ? name.substring(separator + 1) : name;

        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "";

        String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);

        return SAFE_EXTENSIONS.contains(extension) ? extension : "";
    }
}
