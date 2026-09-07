package com.back.global.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageKeyTest {

    @ParameterizedTest
    @DisplayName("파일명에 경로 조각이나 위험한 확장자가 있으면 확장자를 버린다")
    @ValueSource(strings = {
            "file.png/../../malicious",
            "a.png/../../../etc/passwd",
            "../../../etc/passwd",
            "x.png/sub/dir/evil",
            "..\\..\\windows\\system32",
            "avatar.html",
            "avatar.svg",
            "avatar.jsp",
            "avatar.PHP",
            "avatar.gif",
            "avatar.webp",
            "확장자없음"
    })
    void dropsUnsafeExtension(String originalFilename) {
        String key = FileStorage.newKey("profile", originalFilename);

        assertThat(key).matches("^profile/[0-9a-f-]{36}$");
    }

    @ParameterizedTest
    @DisplayName("허용한 이미지 확장자는 그대로 남긴다 (화면과 같은 jpg·png 기준)")
    @ValueSource(strings = {"a.png", "a.PNG", "a.jpg", "a.jpeg"})
    void keepsSafeExtension(String originalFilename) {
        String key = FileStorage.newKey("profile", originalFilename);

        assertThat(key).matches("^profile/[0-9a-f-]{36}\\.(png|jpg|jpeg)$");
    }

    @Test
    @DisplayName("경로가 섞여 있어도 디렉터리 밖으로 나가지 않는다")
    void neverEscapesDirectory() {
        String key = FileStorage.newKey("profile", "evil.png/../../../../etc/passwd");

        assertThat(key).doesNotContain("..").doesNotContain("/etc/");
    }

    @Test
    @DisplayName("파일명이 없으면 확장자 없이 저장한다")
    void allowsNullFilename() {
        assertThat(FileStorage.newKey("profile", null)).matches("^profile/[0-9a-f-]{36}$");
    }
}
