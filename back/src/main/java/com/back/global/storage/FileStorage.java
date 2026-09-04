package com.back.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

public interface FileStorage {

    /** 파일을 저장하고 외부에서 접근할 수 있는 URL 을 돌려준다. */
    String upload(MultipartFile file, String directory);

    /** 원본 확장자만 남긴 랜덤 키. 원본 이름은 그대로 쓰지 않는다. */
    static String newKey(String directory, String originalFilename) {
        int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = dot > -1 ? originalFilename.substring(dot).toLowerCase(Locale.ROOT) : "";

        return directory + "/" + UUID.randomUUID() + extension;
    }
}
