package com.back.global.app;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationVersionHeaderFilterTest {
    @Test
    void addsImageVersionToEveryResponse() throws Exception {
        ApplicationVersionHeaderFilter filter = new ApplicationVersionHeaderFilter("2173eac");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/parties"),
                response,
                new MockFilterChain()
        );

        assertThat(response.getHeader(ApplicationVersionHeaderFilter.HEADER_NAME))
                .isEqualTo("2173eac");
    }
}
