package com.back.domain.member.profile.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileUrlTest {

    private MemberProfile profile() {
        return new MemberProfile(new Member("user1@test.com", null, "유저1", null));
    }

    private void saveLink(String url) {
        profile().modify("닉네임", null, null, null, null, null, null, null,
                List.of(new LinkCommand("깃허브", url)));
    }

    private String savedUrl(String url) {
        MemberProfile profile = profile();
        profile.modify("닉네임", null, null, null, null, null, null, null,
                List.of(new LinkCommand("깃허브", url)));

        return profile.getLinks().getFirst().getUrl();
    }

    @ParameterizedTest
    @DisplayName("http·https 가 아닌 스킴은 거부한다")
    @ValueSource(strings = {
            "javascript://%0aalert(document.domain)",
            "JavaScript://comment%0Aalert(1)",
            "javascript:alert(1)",
            "vbscript://x",
            "data:text/html;base64,PHNjcmlwdD4=",
            "file:///etc/passwd",
            "mailto:me@example.com"
    })
    void rejectsNonHttpScheme(String url) {
        assertThatThrownBy(() -> saveLink(url))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("http 또는 https");
    }

    @ParameterizedTest
    @DisplayName("webpage 도 같은 규칙이라 javascript 스킴을 거부한다")
    @ValueSource(strings = {"javascript://%0aalert(1)", "javascript:alert(1)", "data:text/html,x"})
    void rejectsNonHttpSchemeOnWebpage(String url) {
        assertThatThrownBy(() -> profile().modify("닉네임", url, null, null, null, null, null, null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("http 또는 https");
    }

    @ParameterizedTest
    @DisplayName("http·https 는 그대로, 스킴이 없으면 https 를 붙인다")
    @CsvSource({
            "https://github.com/x,          https://github.com/x",
            "http://blog.example.com,       http://blog.example.com",
            "HTTPS://github.com/x,          HTTPS://github.com/x",
            "github.com/yoonsun9128,        https://github.com/yoonsun9128",
            "example.com:8080/path,         https://example.com:8080/path"
    })
    void keepsOrPrefixes(String input, String expected) {
        assertThat(savedUrl(input.trim())).isEqualTo(expected.trim());
    }
}
