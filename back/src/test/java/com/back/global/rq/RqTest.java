package com.back.global.rq;

import com.back.domain.member.member.service.MemberService;
import com.back.global.app.CustomConfigProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RqTest {

    @Test
    void localCookieIsHostOnlyAndWorksOverHttp() {
        Cookie cookie = issueCookie(false);

        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
    }

    @Test
    void prodCookieIsHostOnlyAndRequiresHttps() {
        Cookie cookie = issueCookie(true);

        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
    }

    private Cookie issueCookie(boolean secure) {
        CustomConfigProperties properties = new CustomConfigProperties();
        properties.getCookie().setSecure(secure);

        MockHttpServletResponse response = new MockHttpServletResponse();
        Rq rq = new Rq(
                new MockHttpServletRequest(),
                response,
                mock(MemberService.class),
                properties
        );

        rq.setCookie("accessToken", "token");

        return response.getCookie("accessToken");
    }
}
